from securesystemslib import keys
import json

def get_key_info(public_key_path):
    with open(public_key_path, 'r', encoding='utf-8') as f:
        public_key_data = f.read().replace('\r\n', '\n')
    
    rsa_key = keys.import_rsakey_from_public_pem(public_key_data)
    keyid = rsa_key["keyid"]
    public_key = rsa_key["keyval"]["public"]
    
    print(f"Key ID: {keyid}")
    print(f"Public Key:\n{public_key}")

if __name__ == "__main__":
    get_key_info("newkey.pub")