A lightweight, offline password manager for Android built with modern security practices and zero external dependencies.
All data is encrypted locally on the device using Android’s native cryptography APIs and protected behind a master password + biometric authentication.

No accounts, no cloud sync, no analytics, no external servers.
Everything stays on your device fully encrypted.

Security Architecture

These are the actual security mechanisms used no exaggeration, just facts:

• AES-256-GCM encryption

Used for encrypting stored credentials.
Authenticated encryption with random IVs for each entry.

• Hardware-backed Android Keystore

Encryption keys are generated and stored in the system keystore.
Keys cannot be extracted by the app.

• PBKDF2 / SHA-256 master password hashing

Master password is never stored.
A slow, salted key-derivation process is used for verification.

• EncryptedSharedPreferences

Used for securely storing metadata and non-credential values.

• SecureRandom

Used for generating strong passwords and random IVs.

• No plaintext storage

Passwords, notes, and fields are always encrypted at rest.

• Biometric authentication

Fingerprint/face unlock via androidx.biometric.

• Auto-lock + screenshot blocking

The app locks itself when closed or backgrounded, and sensitive screens block screenshots.

Note:
If a device is rooted, data is still encrypted, but no Android app can fully guarantee security against a hostile root environment. This is the correct, honest disclaimer.

App Features

Clean Material Design UI

Biometric unlock (fingerprint/face)

Built-in strong password generator

Notes field for each entry

One-tap copy to clipboard

Auto-clear clipboard (system-level behavior)

Add / edit / delete entries easily

Full dark mode support

No internet permission required

Dependencies

The project relies only on official Android libraries nothing external.

androidx.security:security-crypto

androidx.biometric

Jetpack libraries (UI, lifecycle, etc.)

No 3rd-party crypto packages.
No external services.
Everything uses native Android APIs.

First-Time Setup

Open the app

Create a master password

Enable biometrics (optional)

Start adding entries

How It Works ????
Add a password

Tap the “+” button

Enter service / username / password

Use the generator for strong passwords

Save

View a password

Tap an entry

Copy fields with one tap

Delete

Tap the trash icon

Confirm

Built-in Security Behaviors

Master password required

Biometric unlock available

Auto-lock on exit

Local-only storage

Encrypted database

Screenshot protection

No analytics / tracking

Comparison With Cloud-Based Managers

Not a marketing table just reality:

Feature	This App	1Password	Bitwarden
Encryption	AES-256-GCM	AES-256	AES-256
Biometric Unlock	Yes	Yes	Yes
Open Source	Yes	No	Yes
Cost	Free	Paid	Mostly Free
Data Location	On Device Only	Vendor Servers	Vendor Servers
Cloud Sync	No	Yes	Yes

This project favors local security over convenience.
If you need multi-device sync, you won’t get it here by design.

Possible Future Add-ons

(if contributors or you want to expand)

Encrypted offline backup/restore

Import/export

Search

Password strength meter

Tag/category system

Auto-fill support

WearOS companion

Final Note

This app is built around a simple principle:

If the data never leaves the device, the attack surface stays small.

It’s not pretending to beat enterprise security tools.
It’s just a clean, practical, encrypted offline vault that does exactly what it claims nothing more, nothing less.
