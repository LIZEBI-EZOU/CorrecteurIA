# Signature de la release

Le keystore de production n'est jamais stocké dans le dépôt.

Secrets GitHub à créer :
- RELEASE_KEYSTORE_B64 : fichier .jks encodé en base64
- RELEASE_STORE_PASSWORD : mot de passe du keystore
- RELEASE_KEY_ALIAS : alias de la clé
- RELEASE_KEY_PASSWORD : mot de passe de la clé

Le workflow « Android Release » peut être lancé manuellement ou via un tag v* (ex. v1.1.0). Un tag crée aussi une GitHub Release avec l'APK signé.
