# MatelasPro

Application Android de gestion d'entreprise destinée aux matelassiers.

## Fonctionnalités
- 📦 **Gestion de stock** — produits, catégories, mouvements
- 💰 **Ventes** — enregistrement, historique, tableau de bord
- 📉 **Dépenses** — suivi quotidien et mensuel
- 🤝 **Fournisseurs** — crédits, paiements, historique
- 📋 **Gestion ALU** — produits ALU, devis, notes de débit
- 👥 **Utilisateurs** — rôles admin/utilisateur, création de comptes
- 📊 **Bénéfices** — calcul automatique (ventes − dépenses)
- 🌙 **Mode sombre**

## Stack technique
- Kotlin, Android (minSdk 26)
- Firebase Auth (connexion email/mot de passe)
- Firebase Firestore (base de données temps réel)
- ViewBinding, Coroutines, Flow
- Shimmer (skeleton loading)

## Architecture
Cloud‑first — pas de stockage local. Toutes les données sont synchronisées en temps réel via Firestore.
