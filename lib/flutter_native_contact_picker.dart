import 'dart:async';
import 'dart:io';

import 'package:flutter/services.dart';

class FlutterContactPicker {
  static const MethodChannel _channel =
      const MethodChannel('flutter_native_contact_picker');

  /// Method to call native code and get contact detail
  Future<Contact?> selectContact() async {
    final Map<dynamic, dynamic>? result =
        await _channel.invokeMethod('selectContact');
    if (result == null) {
      return null;
    }
    return new Contact.fromMap(result);
  }

  Future<List<Contact>?> selectContacts() async {
    if (!Platform.isIOS) throw UnimplementedError();
    final List<dynamic>? result = await _channel.invokeMethod('selectContacts');
    return result?.map((e) => Contact.fromMap(e)).toList();
  }
}

/// Represents a contact selected by the user.
class Contact {
  Contact({
    this.fullName,
    this.firstName,
    this.lastName,
    this.phoneNumbers,
    this.emailAddresses,
    this.postalAddresses,
  });

  factory Contact.fromMap(Map<dynamic, dynamic> map) {
    return Contact(
      fullName: map['fullName'],
      firstName: map['givenName'],
      lastName: map['familyName'],
      phoneNumbers: map['phoneNumbers'] != null
          ? (map['phoneNumbers'] as List<dynamic>)
              .map((e) => e.toString())
              .toList()
          : null,
      emailAddresses: map['emailAddresses'] != null
          ? (map['emailAddresses'] as List<dynamic>)
              .map((e) => e.toString())
              .toList()
          : null,
      postalAddresses: map['postalAddresses'] != null
          ? (map['postalAddresses'] as List<dynamic>)
              .map((e) => e.toString())
              .toList()
          : null,
    );
  }

  /// The full name of the contact, e.g. "Jayesh Pansheriya".
  final String? fullName;

  /// The first or given name of the contact, e.g. "Jayesh".
  final String? firstName;

  /// The last or family name of the contact, e.g. "Pansheriya".
  final String? lastName;

  /// Email addresses of the contact.
  final List<String>? emailAddresses;

  /// The phone number of the contact.
  final List<String>? phoneNumbers;

  /// The postal address of the contact.
  final List<String>? postalAddresses;

  @override
  String toString() => '$firstName $lastName ($fullName). Phone: $phoneNumbers, Email: $emailAddresses, Postal: $postalAddresses';
}

/// Represents a phone number selected by the user.
class PhoneNumber {
  PhoneNumber({this.number, this.label});

  factory PhoneNumber.fromMap(Map<dynamic, dynamic> map) =>
      new PhoneNumber(number: map['number'], label: map['label']);

  /// The formatted phone number, e.g. "+1 (555) 555-5555"
  final String? number;

  /// The label associated with the phone number, e.g. "home" or "work".
  final String? label;

  @override
  String toString() => '$number ($label)';
}
