#!/usr/bin/env python3
import os
import subprocess
import sys
import time
import shutil

# --- Auto-Install Dependencies ---
def install_dependencies():
    required = ["click", "rich"]
    installed = False
    for package in required:
        try:
            __import__(package)
        except ImportError:
            print(f"Installing missing dependency: {package}...")
            subprocess.check_call([sys.executable, "-m", "pip", "install", package])
            installed = True
    
    if installed:
        print("Dependencies installed. Restarting script...")
        os.execv(sys.executable, [sys.executable] + sys.argv)

install_dependencies()

# Now safe to import
import click
from rich.console import Console
from rich.table import Table
from rich.panel import Panel
from rich.prompt import Prompt, Confirm

# --- Configuration ---
CLUSTER_NAME = "limitedcart"
NAMESPACE = "limitedcart"
KIND_CONFIG = "kind-config.yaml"
SERVICES = [
    "auth-service",
    "product-service",
    "inventory-service",
    "order-service",
    "payment-service",
    "notification-service",
    "temporal-worker",
    "frontend"
]

SERVICE_ALIASES = {
    "as": "auth-service",
    "ps": "product-service",
    "is": "inventory-service",
    "os": "order-service",
    "pays": "payment-service",
    "ns": "notification-service",
    "tw": "temporal-worker",
    "f": "frontend"
}
# Map service names to their Docker build contexts and Dockerfiles if different
SERVICE_CONFIGS = {
    "frontend": {"context": "./frontend-web", "dockerfile": "Dockerfile"},
}

console = Console()

# --- Helpers ---

def run_command(command, check=True, shell=True, capture_output=False):
    """Runs a shell command and returns the result."""
    try:
        result = subprocess.run(
            command,
            check=check,
            shell=shell,
            text=True,
            capture_output=capture_output
        )
        return result
    except subprocess.CalledProcessError as e:
        console.print(f"[bold red]Error running command:[/bold red] {command}")
        console.print(f"[red]{e.stderr if capture_output else e}[/red]")
        if check:
            raise e
        return None

def check_prerequisites():
    """Checks if required tools are installed."""
    tools = ["kind", "kubectl", "docker"]
    missing = []
    for tool in tools:
        if not shutil.which(tool):
            missing.append(tool)
    
    if missing:
        console.print(f"[bold red]Missing required tools:[/bold red] {', '.join(missing)}")
        console.print("Please install them before running this script.")
        sys.exit(1)
    
    # Check if Docker daemon is running
    try:
        subprocess.run("docker info", shell=True, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    except subprocess.CalledProcessError:
        console.print("[bold red]Docker is not running![/bold red]")
        console.print("Please start Docker Desktop (or your Docker daemon) and try again.")
        sys.exit(1)

    console.print("[green]All prerequisites met.[/green]")

def wait_for_pod_ready(label_selector, namespace=NAMESPACE, timeout=120):
    """Waits for a pod with the given label to be ready."""
    console.print(f"[yellow]Waiting for {label_selector} to be ready in {namespace}...[/yellow]")
    try:
        run_command(
            f"kubectl wait --for=condition=ready pod -l {label_selector} -n {namespace} --timeout={timeout}s",
            check=True
        )
        console.print(f"[green]{label_selector} is ready![/green]")
    except subprocess.CalledProcessError:
        console.print(f"[bold red]Timeout waiting for {label_selector}[/bold red]")

def resolve_service(name):
    """Resolves service alias to full name."""
    return SERVICE_ALIASES.get(name, name)

# --- Actions ---

def bootstrap_cluster():
    """Creates the Kind cluster."""
    if run_command(f"kind get clusters | grep {CLUSTER_NAME}", check=False, capture_output=True).returncode == 0:
        console.print(f"[yellow]Cluster '{CLUSTER_NAME}' already exists.[/yellow]")
        if not Confirm.ask("Do you want to delete it and recreate it?"):
            return

        teardown_cluster()

    console.print(f"[bold blue]Creating Kind cluster '{CLUSTER_NAME}'...[/bold blue]")
    try:
        run_command(f"kind create cluster --config {KIND_CONFIG} --name {CLUSTER_NAME}")
        console.print("[green]Cluster created successfully.[/green]")
        
        # Install Nginx Ingress Controller
        console.print("[blue]Installing Nginx Ingress Controller...[/blue]")
        run_command("kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml")
        
        console.print("[yellow]Waiting for Ingress Controller to be ready (this may take a minute)...[/yellow]")
        wait_for_pod_ready("app.kubernetes.io/component=controller", namespace="ingress-nginx", timeout=180)
    except Exception as e:
        console.print(f"[bold red]Failed to bootstrap cluster: {e}[/bold red]")
        console.print("[yellow]Check if Docker is running and healthy.[/yellow]")

def teardown_cluster():
    """Destroys the Kind cluster."""
    console.print(f"[bold red]Deleting Kind cluster '{CLUSTER_NAME}'...[/bold red]")
    run_command(f"kind delete cluster --name {CLUSTER_NAME}")
    console.print("[green]Cluster deleted.[/green]")

def build_and_load(service_name=None):
    """Builds Docker images and loads them into Kind."""
    services_to_build = [service_name] if service_name else SERVICES
    
    for service in services_to_build:
        console.print(f"[bold blue]Processing {service}...[/bold blue]")
        
        # Determine build context and dockerfile
        config = SERVICE_CONFIGS.get(service, {"context": ".", "dockerfile": "Dockerfile"})
        context = config["context"]
        dockerfile = config["dockerfile"]
        
        # Build Args for backend services (standardizing on module name)
        build_args = ""
        if service != "frontend":
             build_args = f"--build-arg MODULE={service}"

        # Build
        image_tag = f"limitedcart/{service}:latest"
        console.print(f"  Building {image_tag}...")
        try:
            run_command(f"docker build -t {image_tag} -f {context}/{dockerfile} {build_args} {context}")
        except subprocess.CalledProcessError:
            console.print(f"[bold red]Failed to build {service}. Skipping load.[/bold red]")
            continue

        # Load
        console.print(f"  Loading {image_tag} into Kind...")
        run_command(f"kind load docker-image {image_tag} --name {CLUSTER_NAME}")
        console.print(f"[green]  {service} ready.[/green]")

def generate_k8s_secrets():
    """Generates k8s/common/secret.yaml from .env file."""
    console.print("[blue]Generating secrets from .env...[/blue]")
    env_vars = {}
    try:
        with open(".env", "r") as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#"):
                    key, _, value = line.partition("=")
                    env_vars[key.strip()] = value.strip()
    except FileNotFoundError:
        console.print("[yellow].env not found, using default values...[/yellow]")
    
    # Define the secret content dynamically
    secret_content = f"""apiVersion: v1
kind: Secret
metadata:
  name: common-secrets
  namespace: {NAMESPACE}
type: Opaque
stringData:
  POSTGRES_USER: "{env_vars.get('POSTGRES_USER', 'postgres')}"
  POSTGRES_PASSWORD: "{env_vars.get('POSTGRES_PASSWORD', 'postgres')}"
  REDIS_PASSWORD: "{env_vars.get('REDIS_PASSWORD', '')}"
  JWT_SECRET: "{env_vars.get('JWT_SECRET', 'default-secret-key-must-be-changed')}"
  JWT_EXPIRATION: "{env_vars.get('JWT_EXPIRATION', '86400000')}"
  ADMIN_BOOTSTRAP_EMAIL: "{env_vars.get('ADMIN_BOOTSTRAP_EMAIL', 'admin@limitedcart.com')}"
  ADMIN_BOOTSTRAP_PASSWORD: "{env_vars.get('ADMIN_BOOTSTRAP_PASSWORD', 'admin')}"
"""
    os.makedirs("k8s/common", exist_ok=True)
    with open("k8s/common/secret.yaml", "w") as f:
        f.write(secret_content)
    console.print("[green]Secrets generated at k8s/common/secret.yaml[/green]")

def deploy_infrastructure():
    """Deploys common configs and infrastructure."""
    # Check if namespace exists
    if run_command(f"kubectl get namespace {NAMESPACE}", check=False, capture_output=True).returncode != 0:
        console.print(f"[bold blue]Creating Namespace '{NAMESPACE}'...[/bold blue]")
        run_command(f"kubectl create namespace {NAMESPACE}")
    else:
        console.print(f"[blue]Namespace '{NAMESPACE}' already exists.[/blue]")

    run_command("kubectl apply -f k8s/namespace.yaml")
    time.sleep(2)

    console.print("[bold blue]Deploying Metrics Server...[/bold blue]")
    run_command("kubectl apply -f k8s/infrastructure/metrics-server.yaml")

    generate_k8s_secrets()
    console.print("[bold blue]Deploying Common Configs & Secrets...[/bold blue]")
    run_command("kubectl apply -f k8s/common/")
    
    console.print("[bold blue]Deploying Infrastructure (DB, Kafka, Redis, etc.)...[/bold blue]")
    run_command("kubectl apply -f k8s/infrastructure/")
    
    # Wait for Postgres
    console.print("[yellow]Waiting for Postgres to be ready...[/yellow]")
    # Postgres is a StatefulSet, so we wait for the pod-0
    wait_for_pod_ready("app=postgres", timeout=120)
    console.print("[green]Infrastructure deployed.[/green]")

def deploy_services(service_name=None):
    """Deploys microservices."""
    if service_name:
        console.print(f"[bold blue]Deploying {service_name}...[/bold blue]")
        file_path = f"k8s/services/{service_name}.yaml"
        if os.path.exists(file_path):
            run_command(f"kubectl apply -f {file_path}")
            # Restart to pick up new image if it was just loaded
            run_command(f"kubectl rollout restart deployment/{service_name} -n {NAMESPACE}")
            console.print(f"[green]{service_name} deployed.[/green]")
        else:
            console.print(f"[red]Manifest for {service_name} not found at {file_path}[/red]")
    else:
        console.print("[bold blue]Deploying All Microservices...[/bold blue]")
        run_command("kubectl apply -f k8s/services/")
        console.print("[bold blue]Deploying Ingress...[/bold blue]")
        run_command("kubectl apply -f k8s/ingress.yaml")
        console.print("[green]All services deployed.[/green]")

def build_and_deploy_single():
    """Builds and deploys a single service."""
    choices = SERVICES + list(SERVICE_ALIASES.keys())
    svc = Prompt.ask("Select service", choices=choices)
    svc = resolve_service(svc)
    build_and_load(svc)
    deploy_services(svc)

def apply_config():
    """Re-applies ConfigMaps/Secrets and restarts deployments."""
    generate_k8s_secrets()
    console.print("[bold blue]Re-applying ConfigMaps and Secrets...[/bold blue]")
    run_command("kubectl apply -f k8s/common/")
    
    if Confirm.ask("Do you want to restart all deployments to pick up changes?"):
        console.print("[blue]Restarting all deployments...[/blue]")
        run_command(f"kubectl rollout restart deployment -n {NAMESPACE}")
        run_command(f"kubectl rollout restart statefulset -n {NAMESPACE}")
        console.print("[green]Restart triggered.[/green]")

def watch_status():
    """Watches cluster status."""
    console.print("[blue]Starting live dashboard (Ctrl+C to exit)...[/blue]")
    try:
        # Using watch command if available, otherwise just one-time
        if shutil.which("watch"):
            os.system(f"watch -n 2 'kubectl get pods,svc,ingress,hpa -n {NAMESPACE}'")
        else:
            while True:
                os.system("clear")
                run_command(f"kubectl get pods,svc,ingress,hpa -n {NAMESPACE}", check=False)
                time.sleep(2)
    except KeyboardInterrupt:
        pass

def get_logs():
    """Fetches logs for a service."""
    choices = SERVICES + ["postgres", "kafka", "redis"] + list(SERVICE_ALIASES.keys())
    service = Prompt.ask("Enter service name", choices=choices)
    service = resolve_service(service)
    
    follow = Confirm.ask("Follow logs?", default=True)
    flag = "-f" if follow else ""
    
    # Find pod name
    try:
        cmd = f"kubectl get pods -l app={service} -n {NAMESPACE} -o jsonpath='{{.items[0].metadata.name}}'"
        result = run_command(cmd, capture_output=True)
        pod_name = result.stdout.strip()
        if not pod_name:
            console.print(f"[red]No running pod found for {service} in namespace {NAMESPACE}[/red]")
            return
        
        os.system(f"kubectl logs {pod_name} -n {NAMESPACE} {flag}")
    except Exception as e:
        console.print(f"[red]Error fetching logs: {e}[/red]")

# --- Main Menu ---

def watch_node_metrics():
    """Live view of node metrics."""
    try:
        while True:
            console.clear()
            console.print("[bold blue]--- Node Metrics (Ctrl+C to exit) ---[/bold blue]")
            run_command("kubectl top nodes", check=False)
            time.sleep(2)
    except KeyboardInterrupt:
        pass

def watch_pod_metrics():
    """Live view of pod metrics."""
    try:
        while True:
            console.clear()
            console.print(f"[bold blue]--- Pod Metrics ({NAMESPACE}) (Ctrl+C to exit) ---[/bold blue]")
            run_command(f"kubectl top pods -n {NAMESPACE}", check=False)
            time.sleep(2)
    except KeyboardInterrupt:
        pass

@click.command()
def main():
    """LimitedCart Kubernetes Management CLI"""
    check_prerequisites()
    
    while True:
        console.print("\n[bold cyan]--- LimitedCart K8s Manager ---[/bold cyan]")
        table = Table(show_header=False, box=None)
        table.add_row("1. Bootstrap Cluster", "Create Kind cluster & install Ingress")
        table.add_row("2. Build & Load All", "Build all images and load into Kind")
        table.add_row("3. Deploy Infrastructure", "Deploy DB, Kafka, Redis, etc.")
        table.add_row("4. Deploy All Services", "Deploy all microservices & Ingress")
        table.add_row("5. Build & Deploy Single", "Build, Load & Deploy a specific service")
        table.add_row("6. Apply Config Changes", "Update ConfigMaps/Secrets & Restart")
        table.add_row("7. Watch Status", "Live view of pods/services")
        table.add_row("8. View Logs", "View logs for a service")
        table.add_row("9. Watch Node Metrics", "Live node CPU/Memory usage")
        table.add_row("10. Watch Pod Metrics", "Live pod CPU/Memory usage")
        table.add_row("11. Teardown Cluster", "Delete Kind cluster")
        table.add_row("qw. Exit", "Exit the CLI")
        console.print(table)
        
        choice = Prompt.ask("Select an option", default="7")
        
        if choice == "qw":
            console.print("Goodbye!")
            break
        elif choice == "1":
            bootstrap_cluster()
        elif choice == "2":
            build_and_load()
        elif choice == "3":
            deploy_infrastructure()
        elif choice == "4":
            deploy_services()
        elif choice == "5":
            build_and_deploy_single()
        elif choice == "6":
            apply_config()
        elif choice == "7":
            watch_status()
        elif choice == "8":
            get_logs()
        elif choice == "9":
            watch_node_metrics()
        elif choice == "10":
            watch_pod_metrics()
        elif choice == "11":
            if Confirm.ask("Are you sure you want to delete the cluster?"):
                teardown_cluster()
        else:
            console.print("[red]Invalid option[/red]")

if __name__ == "__main__":
    main()
