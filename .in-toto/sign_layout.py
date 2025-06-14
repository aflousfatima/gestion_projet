import json
import in_toto.models.layout
import in_toto.models.metadata
from securesystemslib import keys
from securesystemslib.exceptions import CryptoError, FormatError

def normalize_key(key):
    """Normalise une clé PEM en supprimant les espaces et en unifiant les retours à la ligne."""
    return ''.join(key.replace('\r\n', '\n').replace('\n', '').split())

def sign_layout(layout_path, private_key_path, output_path):
    try:
        # Charge le fichier root.layout
        print(f"Chargement de {layout_path}...")
        with open(layout_path, 'r', encoding='utf-8') as f:
            layout_data = json.load(f)
        print("JSON chargé avec succès.")

        # Valide le layout
        print("Validation du schéma du layout...")
        layout = in_toto.models.layout.Layout.read(layout_data)
        if layout is None:
            raise ValueError("Layout.read() a retourné None, vérifiez le schéma du fichier.")
        print("Schéma validé avec succès.")

        # Charge la clé privée
        print(f"Chargement de la clé privée {private_key_path}...")
        with open(private_key_path, 'r', encoding='utf-8') as f:
            private_key_data = f.read().replace('\r\n', '\n')

        # Importe la clé privée RSA pour obtenir le keyid
        rsa_key = keys.import_rsakey_from_private_pem(private_key_data)
        keyid = rsa_key["keyid"]
        public_key = rsa_key["keyval"]["public"].replace('\r\n', '\n')

        # Vérifie la correspondance avec la clé publique dans root.layout
        expected_keyid = "d6982dd865aeacdd6febd1acbea1730a2476ad55c8d387ef33c0ad3066773e14"
        layout_key = layout.keys.get(expected_keyid)
        if not layout_key:
            raise ValueError(f"Clé publique avec keyid {expected_keyid} non trouvée dans root.layout")
        
        normalized_public_key = normalize_key(public_key)
        normalized_layout_key = normalize_key(layout_key["keyval"]["public"])
        
        if normalized_public_key != normalized_layout_key:
            raise ValueError(f"La clé privée ne correspond pas à la clé publique avec keyid {expected_keyid}")

        # Crée un objet Metadata
        print("Création de l'objet Metadata...")
        metadata = in_toto.models.metadata.Metablock(signed=layout)

        # Signature via in-toto
        print("Signature du layout via in-toto...")
        metadata.sign(rsa_key)

        # Sauvegarde le fichier signé
        print(f"Sauvegarde du fichier signé dans {output_path}...")
        metadata.dump(output_path)

        print("Signature réussie ! Vérifiez la section 'signatures' dans", output_path)

    except FileNotFoundError as e:
        print(f"Erreur : Fichier non trouvé - {e}")
    except json.JSONDecodeError as e:
        print(f"Erreur : Fichier JSON invalide - {e}")
    except FormatError as e:
        print(f"Erreur : Format de fichier ou clé invalide - {e}")
    except CryptoError as e:
        print(f"Erreur : Problème cryptographique (clé privée invalide) - {e}")
    except ValueError as e:
        print(f"Erreur de validation : {str(e)}")
    except Exception as e:
        import traceback
        print(f"Erreur inattendue : {str(e)}")
        print("Trace complète :")
        traceback.print_exc()

if __name__ == "__main__":
    sign_layout(
        layout_path="root.layout",
        private_key_path="private.key",
        output_path="root1.layout"
    )
