# Architecture
UI Compose → ViewModel → moteur de correction.
- LanguageTool : moteur linguistique local.
- OfflineFrenchCorrector : fallback conservateur.
- OnDeviceAi : Gemini Nano / ML Kit si disponible.
- Aucun cloud n’est activé par défaut.
