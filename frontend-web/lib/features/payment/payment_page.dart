import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'dart:html' as html show window;
import '../../shared/navbar.dart';
import '../orders/order_provider.dart';

class PaymentPage extends StatefulWidget {
  final String? orderId;
  final String? token;

  const PaymentPage({super.key, this.orderId, this.token});

  @override
  State<PaymentPage> createState() => _PaymentPageState();
}

class _PaymentPageState extends State<PaymentPage> {
  final _cardNumberController = TextEditingController(text: "1234123412341234");
  final _expiryController = TextEditingController(text: "12/36");
  final _cvvController = TextEditingController(text: "234");
  final _cardHolderController = TextEditingController(text: "Abhishek Jain");
  final _formKey = GlobalKey<FormState>();

  @override
  void dispose() {
    _cardNumberController.dispose();
    _expiryController.dispose();
    _cvvController.dispose();
    _cardHolderController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final orderProvider = context.watch<OrderProvider>();

    return Scaffold(
      appBar: const Navbar(),
      backgroundColor: Colors.grey[50],
      body: Center(
        child: Container(
          constraints: const BoxConstraints(maxWidth: 500),
          padding: const EdgeInsets.all(32),
          child: Card(
            elevation: 0,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
              side: BorderSide(color: Colors.grey[200]!),
            ),
            child: Padding(
              padding: const EdgeInsets.all(32),
              child: Form(
                key: _formKey,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Secure Payment',
                      style: TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFF111827),
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      'Complete your purchase for Order #${widget.orderId ?? orderProvider.currentOrder?.id ?? "Unknown"}',
                      style: TextStyle(
                        fontSize: 14,
                        color: Colors.grey[600],
                      ),
                    ),
                    const SizedBox(height: 24),

                    TextFormField(
                      controller: _cardHolderController,
                      decoration: const InputDecoration(
                        labelText: 'Cardholder Name',
                        border: OutlineInputBorder(),
                      ),
                      validator: (value) {
                        if (value == null || value.isEmpty) return 'Required';
                        return null;
                      },
                    ),
                    const SizedBox(height: 24),

                    // Card Number
                    TextFormField(
                      controller: _cardNumberController,
                      decoration: const InputDecoration(
                        labelText: 'Card Number',
                        border: OutlineInputBorder(),
                        prefixIcon: Icon(Icons.credit_card),
                      ),
                      validator: (value) {
                        if (value == null || value.isEmpty) return 'Required';
                        if (value.length < 16) return 'Invalid card number';
                        return null;
                      },
                    ),
                    const SizedBox(height: 24),
                    
                    Row(
                      children: [
                        // Expiry
                        Expanded(
                          child: TextFormField(
                            controller: _expiryController,
                            decoration: const InputDecoration(
                              labelText: 'MM/YY',
                              border: OutlineInputBorder(),
                              hintText: '12/25',
                            ),
                            validator: (value) {
                              if (value == null || value.isEmpty) return 'Required';
                              return null;
                            },
                          ),
                        ),
                        const SizedBox(width: 24),
                        // CVV
                        Expanded(
                          child: TextFormField(
                            controller: _cvvController,
                            obscureText: true,
                            decoration: const InputDecoration(
                              labelText: 'CVV',
                              border: OutlineInputBorder(),
                              prefixIcon: Icon(Icons.lock_outline),
                            ),
                            validator: (value) {
                              if (value == null || value.isEmpty) return 'Required';
                              if (value.length < 3) return 'Invalid CVV';
                              return null;
                            },
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 32),
                    
                    SizedBox(
                      width: double.infinity,
                      height: 50,
                      child: ElevatedButton(
                        onPressed: orderProvider.isProcessingPayment
                            ? null
                            : () async {
                                if (_formKey.currentState!.validate()) {
                                  final token = widget.token;
                                  if (token == null) {
                                    ScaffoldMessenger.of(context).showSnackBar(
                                      const SnackBar(content: Text('No active payment token. Start from the order page.')),
                                    );
                                    return;
                                  }

                                  final expiryParts = _expiryController.text.split('/');
                                  if (expiryParts.length != 2) {
                                    ScaffoldMessenger.of(context).showSnackBar(
                                      const SnackBar(content: Text('Expiry must be in MM/YY format')),
                                    );
                                    return;
                                  }

                                  final expiryMonth = expiryParts[0].padLeft(2, '0');
                                  final expiryYear = expiryParts[1].padLeft(2, '0');

                                    try {
                                      await orderProvider.processPayment(
                                        token: token,
                                        cardNumber: _cardNumberController.text,
                                        expiryMonth: expiryMonth,
                                        expiryYear: expiryYear,
                                        cvc: _cvvController.text,
                                        cardHolderName: _cardHolderController.text,
                                      );
                                      if (context.mounted) {
                                        ScaffoldMessenger.of(context).showSnackBar(
                                          const SnackBar(content: Text('Payment Successful!')),
                                        );
                                        
                                        // Close window/tab for web, exit app for mobile
                                        // if (kIsWeb) {
                                        //   // For web: close the window/tab
                                        //
                                        // }
                                        html.window.close();
                                      }
                                    } catch (e) {
                                      if (context.mounted) {
                                        ScaffoldMessenger.of(context).showSnackBar(
                                          SnackBar(content: Text('Payment Failed: $e')),
                                        );
                                      }
                                    }
                                }
                              },
                        style: ElevatedButton.styleFrom(
                          backgroundColor: const Color(0xFF4F46E5),
                          foregroundColor: Colors.white,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(8),
                          ),
                        ),
                        child: orderProvider.isProcessingPayment
                            ? const CircularProgressIndicator(color: Colors.white)
                            : const Text(
                                'Pay Now',
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
