import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../../shared/navbar.dart';
import 'order_provider.dart';

import 'package:flutter/foundation.dart';
import 'package:url_launcher/url_launcher.dart';

class OrderTrackingPage extends StatefulWidget {
  final String orderId;

  const OrderTrackingPage({super.key, required this.orderId});

  @override
  State<OrderTrackingPage> createState() => _OrderTrackingPageState();
}

class _OrderTrackingPageState extends State<OrderTrackingPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<OrderProvider>().fetchOrder(widget.orderId);
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: const Navbar(),
      backgroundColor: Colors.grey[50],
      body: Consumer<OrderProvider>(
        builder: (context, provider, child) {
          final order = provider.currentOrder;
          final updates = provider.updates;

          if (order == null || order.id != widget.orderId) {
            return const Center(child: CircularProgressIndicator());
          }

          return Center(
            child: Container(
              constraints: const BoxConstraints(maxWidth: 800),
              padding: const EdgeInsets.all(32),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        'Order #${order.id}',
                        style: const TextStyle(
                          fontSize: 24,
                          fontWeight: FontWeight.bold,
                          color: Color(0xFF111827),
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                        decoration: BoxDecoration(
                          color: const Color(0xFFEEF2FF),
                          borderRadius: BorderRadius.circular(16),
                          border: Border.all(color: const Color(0xFF6366F1)),
                        ),
                        child: Text(
                          order.status,
                          style: const TextStyle(
                            color: Color(0xFF4F46E5),
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 32),

                  // Timeline
                  Expanded(
                    child: ListView.builder(
                      itemCount: updates.length,
                      itemBuilder: (context, index) {
                        final update = updates[index];
                        final isFirst = index == 0;

                        return Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Column(
                              children: [
                                Container(
                                  width: 16,
                                  height: 16,
                                  decoration: BoxDecoration(
                                    color: isFirst ? const Color(0xFF4F46E5) : Colors.grey[300],
                                    shape: BoxShape.circle,
                                    border: Border.all(
                                      color: Colors.white,
                                      width: 3,
                                    ),
                                    boxShadow: isFirst ? [
                                      BoxShadow(
                                        color: const Color(0xFF4F46E5).withOpacity(0.4),
                                        blurRadius: 8,
                                        spreadRadius: 2,
                                      )
                                    ] : [],
                                  ),
                                ),
                                if (index != updates.length - 1)
                                  Container(
                                    width: 2,
                                    height: 60,
                                    color: Colors.grey[200],
                                  ),
                              ],
                            ),
                            const SizedBox(width: 16),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    update.status.replaceAll('_', ' '),
                                    style: TextStyle(
                                      fontSize: 16,
                                      fontWeight: FontWeight.bold,
                                      color: isFirst ? const Color(0xFF111827) : Colors.grey[500],
                                    ),
                                  ),
                                  const SizedBox(height: 4),
                                  Text(
                                    _formatDate(update.occurredAt),
                                    style: TextStyle(
                                      fontSize: 14,
                                      color: Colors.grey[500],
                                    ),
                                  ),
                                  if (update.message != null) ...[
                                    const SizedBox(height: 8),
                                    Text(
                                      update.message!,
                                      style: TextStyle(
                                        fontSize: 14,
                                        color: Colors.grey[700],
                                      ),
                                    ),
                                  ],
                                  const SizedBox(height: 24),
                                ],
                              ),
                            ),
                          ],
                        );
                      },
                    ),
                  ),
                  // Actions
                  if (order.status == 'PAYMENT_PENDING')
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.only(top: 24),
                      child: ElevatedButton(
                        onPressed: () async {
                          final paymentLink = await context.read<OrderProvider>().initiatePayment(order.id);
                          if (!context.mounted) return;
                          if (paymentLink == null) {
                            ScaffoldMessenger.of(context).showSnackBar(
                              const SnackBar(content: Text('Unable to initiate payment right now')),
                            );
                            return;
                          }
                          
                          final uri = Uri.parse(paymentLink);
                          if (kIsWeb) {
                             await launchUrl(uri, webOnlyWindowName: '_blank');
                          } else {
                             await launchUrl(uri, mode: LaunchMode.externalApplication);
                          }
                        },
                        style: ElevatedButton.styleFrom(
                          backgroundColor: const Color(0xFF4F46E5),
                          foregroundColor: Colors.white,
                          padding: const EdgeInsets.symmetric(vertical: 16),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(8),
                          ),
                        ),
                        child: const Text('Pay Now'),
                      ),
                    ),
                  // Cancel Button (for PENDING, INVENTORY_RESERVED, PAYMENT_PENDING)
                  if (_canCancelOrder(order.status))
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.only(top: 12),
                      child: OutlinedButton(
                        onPressed: () async {
                          final confirmed = await showDialog<bool>(
                            context: context,
                            builder: (context) => AlertDialog(
                              title: const Text('Cancel Order'),
                              content: const Text('Are you sure you want to cancel this order?'),
                              actions: [
                                TextButton(
                                  onPressed: () => Navigator.pop(context, false),
                                  child: const Text('No'),
                                ),
                                TextButton(
                                  onPressed: () => Navigator.pop(context, true),
                                  style: TextButton.styleFrom(foregroundColor: Colors.red),
                                  child: const Text('Yes, Cancel'),
                                ),
                              ],
                            ),
                          );

                          if (confirmed == true && context.mounted) {
                            try {
                              await context.read<OrderProvider>().cancelOrder(order.id);
                              if (context.mounted) {
                                ScaffoldMessenger.of(context).showSnackBar(
                                  const SnackBar(content: Text('Order cancelled successfully')),
                                );
                              }
                            } catch (e) {
                              if (context.mounted) {
                                ScaffoldMessenger.of(context).showSnackBar(
                                  SnackBar(content: Text('Failed to cancel order: $e')),
                                );
                              }
                            }
                          }
                        },
                        style: OutlinedButton.styleFrom(
                          foregroundColor: Colors.red,
                          side: const BorderSide(color: Colors.red),
                          padding: const EdgeInsets.symmetric(vertical: 16),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(8),
                          ),
                        ),
                        child: const Text('Cancel Order'),
                      ),
                    ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  bool _canCancelOrder(String status) {
    return status == 'PENDING' || 
           status == 'INVENTORY_RESERVED' || 
           status == 'PAYMENT_PENDING';
  }

  String _formatDate(DateTime timestamp) =>
      DateFormat('MMM d, y h:mm a').format(timestamp.toLocal());
}
