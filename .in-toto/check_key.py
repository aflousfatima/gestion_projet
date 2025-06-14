from securesystemslib import keys

with open("private.key", "r") as f:
    private_key_data = f.read()

rsa_key = keys.import_rsakey_from_private_pem(private_key_data)
print(f"KeyID: {rsa_key['keyid']}")