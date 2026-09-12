# Expression des besoins (V.1)

## Sommaire

- [1. Objectif du document](#1-objectif-du-document)
- [2. Présentation](#2-présentation)
    - [2.1 Présentation du projet](#21-présentation-du-projet)
    - [2.2 Les contraintes](#22-les-contraintes)
- [3. Acteurs](#3-acteurs)
- [4. Cas d'utilisation](#4-cas-dutilisation)
    - [4.1 Vue d'ensemble](#41-vue-densemble)
    - [4.2 Authentification](#42-authentification)
    - [4.3 Profil et compte Steam](#43-profil-et-compte-steam)
    - [4.4 Administration](#44-administration)
    - [4.5 Gacha](#45-gacha)
    - [4.6 Inventaire et Level Up](#46-inventaire-et-level-up)
    - [4.7 Deck](#47-deck)
    - [4.8 Marché](#48-marché)
    - [4.9 Classement](#49-classement)
- [5. Annexes](#5-annexes)
    - [5.1 Terminologie](#51-terminologie)

---

## 1. Objectif du document

Ce document contient l'expression des besoins du projet **Bruine**, application web de gamification Steam développée
dans le cadre du cours GLG204 au CNAM.

---

## 2. Présentation

### 2.1 Présentation du projet

**Bruine** est une application web de gamification liée à la plateforme Steam. Elle permet aux utilisateurs de se
connecter via leur compte Steam et de participer à un système de TCG (cartes à collectionner, en bon francais) :

- **Currency** : chaque heure joué sur Steam rapporte une certaine quantité de points Bruine.
- **Gacha** : dépenser ses points pour tirer des cartes aléatoires de différentes raretés et finitions.
- **Inventaire** : consulter ses cartes collectées.
- **Level Up** : convertir des cartes en XP Bruine selon leur rareté et leur finition.
- **Deck** : composer un deck de 10 cartes, visible publiquement via un widget intégrable en tant qu'image.
- **Marché** : acheter et vendre des cartes entre utilisateurs du site.
- **Classement** : se mesurer aux autres joueurs par temps de jeu Steam ou par XP Bruine.

Un dashboard est accessible aux administrateurs Bruine pour gérer les comptes administrateurs, les utilisateurs Steam et
le catalogue de récompenses Gacha. Le dashboard permet aussi aux administrateurs de forcer la connection sur un
utilisateur sans passer par la connection steam.

### 2.2 Les contraintes

- L'authentification des utilisateurs Steam est déléguée à Steam via OpenID 2.0 - Bruine ne stocke aucun mot de passe
  Steam.
- L'inscription d'un utilisateur Steam est automatique à la première connexion.
- Deux niveaux d'utilisateurs coexistent : **utilisateurs Steam** et **administrateurs Bruine**. Leurs sessions sont *
  *isolées** : un même navigateur peut être connecté simultanément aux deux. Le logout ne doit pas déconnecter les deux
  sessions en même temps.
- La currency est la monnaie du gacha et aussi du marché.
- Une carte présente dans le deck ne peut ni être vendue sur le marché, ni être convertie en XP.
- Le widget est une page HTML autonome intégrable en `<img>` sur n'importe quel site, markdown...,
- Les données de profil Steam (avatar, nom d'utilisateur, bibliothèque de jeux) sont récupérées à la demande via l'API
  publique Steam pour ne pas surcharger notre bdd.
- Un administrateur ne peut pas supprimer son propre compte dans le dashboard.

---

## 3. Acteurs

~~~plantuml
@startuml
skinparam backgroundColor #EEEBDC
:"Visiteur":
:"Utilisateur Steam":
:"Administrateur":
@enduml
~~~

| Acteur                | Description                                                                                                                                       |
|-----------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| **Visiteur**          | Toute personne accédant à l'application sans être authentifiée.                                                                                   |
| **Utilisateur Steam** | Utilisateur ayant complété la connection Steam sur le site. Inscrit automatiquement sur Bruine à la première connexion.                           |
| **Administrateur**    | Utilisateur ayant complété la connection via le formulaire admin. Gère les comptes admin, les utilisateurs steam et autre param de l'application. |

---

## 4. Cas d'utilisation

### 4.1 Vue d'ensemble

~~~plantuml
@startuml
left to right direction
    skinparam backgroundColor #EEEBDC

:Visiteur: --> (Se connecter via Steam)
:Visiteur: --> (Accéder à l'accueil)
:Visiteur: --> (Consulter un profil Bruine/Steam)
:Visiteur: --> (Consulter le classement)

:Utilisateur Steam: --> (Accéder à l'accueil)
:Utilisateur Steam: --> (Consulter un profil Steam)
:Utilisateur Steam: --> (Se déconnecter de Bruine)
:Utilisateur Steam: --> (Supprimer son compte Bruine)
:Utilisateur Steam: --> (Effectuer un tirage Gacha)
:Utilisateur Steam: --> (Consulter son inventaire)
:Utilisateur Steam: --> (Convertir des cartes en XP)
:Utilisateur Steam: --> (Gérer son deck)
:Utilisateur Steam: --> (Vendre une carte)
:Utilisateur Steam: --> (Acheter une carte)
:Utilisateur Steam: --> (Annuler sa mise en vente)
:Utilisateur Steam: --> (Consulter le classement)

:Administrateur: --> (Se connecter/déconnecter au dashboard)
:Administrateur: --> (Gérer les comptes admin)
:Administrateur: --> (Gérer les utilisateurs Steam)
:Administrateur: --> (Gérer les récompenses Gacha)
@enduml
~~~

---

### 4.2 Authentification

~~~plantuml
@startuml
skinparam backgroundColor #EEEBDC

state "Visiteur" as Visiteur {
  [*] --> NonConnecte
  NonConnecte --> Connecte : Se connecter via Steam
  Connecte --> NonConnecte : Se déconnecter
}

state "Administrateur" as Admin {
  [*] --> NonConnecteAdmin
  NonConnecteAdmin --> ConnecteAdmin : Se connecter
  ConnecteAdmin --> NonConnecteAdmin : Se déconnecter
}
@enduml
~~~

#### 4.2.1 Se connecter via Steam

**Description** - Permet à un visiteur de s'authentifier sur Bruine via Steam OpenID 2.0. Si c'est la première
connexion, un compte Bruine est automatiquement créé.

**Acteurs** - Visiteur | Utilisateur Steam (reconnexion)

**Pré-conditions** - L'utilisateur doit posséder un compte Steam valide. Steam doit être joignable depuis Internet (
auth/callback OpenID).

**Post-conditions** - L'utilisateur est authentifié « Utilisateur Steam ». Si première connexion : compte Bruine créé.

**Contrainte non fonctionnelle** - OpenID 2.0 garantit qu'aucun mot de passe Steam n'est transmis à Bruine.

~~~plantuml
@startuml
    skinparam backgroundColor #EEEBDC
    skinparam ranksep 30
start
:cliquer sur « Se connecter avec Steam »;
:rediriger vers Steam OpenID;
-> confirmation d'identité reçue;
:valider la confirmation Steam;
if (confirmation valide?) then (non)
    :rediriger vers la page d'erreur;
    stop
else (oui)
endif
:identifier l'utilisateur;
if (compte Bruine existant?) then (non)
    :créer le compte Bruine;
endif
:ouvrir la session utilisateur;
:rediriger vers /;
stop
@enduml
~~~

#### 4.2.2 Se déconnecter de Steam / du back-office

**Description** - Permet à un utilisateur ou administrateur de fermer sa session sans affecter l'autre session active
dans le même navigateur.

**Contrainte non fonctionnelle** - La déconnexion ne doit jamais invalider la session de l'autre rôle.

#### 4.2.3 Se connecter au dashboard admin

**Description** - Permet à un administrateur de s'authentifier via un formulaire identifiant/mot de passe distinct du
flux Steam.

**Contraintes non fonctionnelles** - Les mots de passe ne sont pas stockés en clair.

~~~plantuml
@startuml
    skinparam backgroundColor #EEEBDC
    skinparam ranksep 30
start
:accéder au dashboard;
if (déjà authentifié admin?) then (oui)
    :afficher dashboard;
    stop
else (non)
endif
repeat :afficher formulaire de connexion;
-> identifiant et mot de passe entrés;
:vérifier les identifiants;
backward:afficher message d'erreur;
repeat while (identifiants invalides?) is (oui) not (non)
:login admin;
:afficher dashboard;
stop
@enduml
~~~

---

### 4.3 Profil et compte Steam

~~~plantuml
@startuml
left to right direction
    skinparam backgroundColor #EEEBDC
:Visiteur: --> (Consulter un profil Steam)
:Utilisateur Steam: --> (Consulter un profil Steam)
:Utilisateur Steam: --> (Supprimer son compte Bruine)
@enduml
~~~

#### 4.3.1 Consulter un profil Steam

**Description** - Affiche le profil public d'un joueur Steam : avatar, nom, bibliothèque de jeux (uniquement les jeux
avec plus d'une heure de jeu pour éviter de faire trop de requête sur l'api). Si le joueur est inscrit sur Bruine,
l'inscription est affiché via un badge. Le propriétaire du profil voit aussi un bouton de suppression de son compte.

**Acteurs** - Visiteur | Utilisateur Steam

**Pré-conditions** - L'identifiant Steam dans l'URL doit être valide (17 chiffres). L'API Steam
doit être accessible.

**Flux normal**

1. Réception de l'identifiant Steam ;
2. Validation du format ;
3. Récupération du profil via l'API Steam ;
4. Vérification de l'inscription sur Bruine ;
5. Si deck existant : affichage du widget deck (iframe) ;
6. Affichage du profil ; chargement asynchrone de la bibliothèque de jeux.

**Exceptions** - format invalide ou erreur API -> redirection vers page d'erreur.

**Contraintes non fonctionnelles** - Seuls les jeux avec >= 60 min de temps de jeu sont affichés, triés par temps
décroissant.

~~~plantuml
@startuml
    skinparam backgroundColor #EEEBDC
    skinparam ranksep 30
start
:recevoir l'identifiant Steam;
if (format valide?) then (non)
    :rediriger vers la page d'erreur;
    stop
else (oui)
endif
:récupérer le profil via l'API Steam;
if (erreur API?) then (oui)
    :rediriger vers la page d'erreur;
    stop
else (non)
endif
:vérifier inscription sur Bruine;
if (joueur inscrit et a un deck?) then (oui)
    :afficher badge + deck;
endif
:afficher profil;
:charger la bibliothèque de jeux (async);
stop
@enduml
~~~

#### 4.3.2 Supprimer son compte Bruine

**Description** - Permet à un utilisateur Steam de supprimer ses données Bruine. Le système vérifie que l'utilisateur
agit sur son propre compte, supprime le compte, ferme la session Steam, puis redirige vers l'écran d'accueil.

**Pré-conditions** - Authentifié. Profil consulté = compte connecté.

**Flux alternatif** - tentative de suppression d'un autre compte -> redirection vers la page d'erreur.

~~~plantuml
@startuml
    skinparam backgroundColor #EEEBDC
    skinparam ranksep 30
start
:cliquer sur « Supprimer mon compte »;
if (authentifié ?) then (non)
    :rediriger vers la page d'erreur;
    stop
else (oui)
endif
if (profil = compte connecté?) then (non)
    :rediriger vers la page d'erreur;
    stop
else (oui)
endif
:supprimer le compte Bruine;
:fermer la session Steam;
:rediriger vers accueil;
stop
@enduml
~~~

---

### 4.4 Administration

~~~plantuml
@startuml
left to right direction
    skinparam backgroundColor #EEEBDC
:Administrateur: --> (Gérer les comptes admin)
:Administrateur: --> (Gérer les utilisateurs Steam)
:Administrateur: --> (Gérer les récompenses Gacha)
:Administrateur: --> (Gérer les paramètres de l'application)
(Gérer les comptes admin) .> (Créer un compte admin) : <<include>>
(Gérer les comptes admin) .> (Modifier le mot de passe) : <<include>>
(Gérer les comptes admin) .> (Supprimer un compte admin) : <<include>>
(Gérer les utilisateurs Steam) .> (Consulter la liste) : <<include>>
(Gérer les utilisateurs Steam) .> (Supprimer un utilisateur) : <<include>>
(Gérer les récompenses Gacha) .> (Créer une récompense) : <<include>>
(Gérer les récompenses Gacha) .> (Modifier une récompense) : <<include>>
(Gérer les récompenses Gacha) .> (Supprimer une récompense) : <<include>>
(Gérer les paramètres de l'application) .> (Modifier les probabilités du gacha, ...) : <<include>>
@enduml
~~~

#### 4.4.1 Gérer les comptes administrateur

**Description** - Opérations CRUD sur les comptes admin : consultation, création, modification du mot de passe,
suppression. **Un administrateur ne peut pas supprimer son propre compte** (protection côté serveur ; bouton désactivé
dans l'interface).

**Pré-conditions** - Authentifié en tant qu'administrateur.

**Flux alternatifs** - nom d'utilisateur déjà pris lors d'une création : message d'erreur. Tentative
d'auto-suppression : rejetée.

~~~plantuml
@startuml
    skinparam backgroundColor #EEEBDC
    skinparam ranksep 30
start
:afficher la liste des comptes admin;
repeat :choisir une action;
switch (action?)
case (créer)
    :soumettre formulaire (identifiant, mot de passe);
    if (identifiant déjà existant?) then (oui)
        :afficher erreur;
    else (non)
        :créer le compte;
    endif
case (modifier mot de passe)
    :soumettre nouveau mot de passe;
    :mettre à jour la bdd;
case (supprimer)
    if (compte = compte connecté?) then (oui)
        :afficher erreur auto-suppression;
    else (non)
        :supprimer le compte;
    endif
endswitch
repeat while ()
stop
@enduml
~~~

#### 4.4.2 Gérer les utilisateurs Steam inscrits

**Description** - Consultation de la liste des utilisateurs Steam inscrits sur Bruine et suppression de leur compte. Il
est aussi possible via button de forcer la connexion sur un compte utilisateur Steam sans passer par le flux
d'authentification.
Steam (utile pour le support, les tests...).

**Remarque** - La suppression par un administrateur ne déconnecte pas immédiatement l'utilisateur concerné. (à voir +
tard)

#### 4.4.3 Gérer les récompenses Gacha

**Description** - CRUD sur le catalogue de cartes disponibles : emoji (des emoji sont utilisés pour simplifier la
production d'asset, mais dans l'idéale, il s'agirait d'image), nom, rareté (commune -> légendaire), description.
Détermine l'ensemble des cartes qu'un joueur peut obtenir lors d'un tirage.

**Pré-conditions** - Authentifié en tant qu'administrateur.

**Flux normal : créer une récompense/carte**

1. L'administrateur remplit le formulaire (emoji, nom, rareté, description) ;
2. Le système enregistre la nouvelle récompense, disponible immédiatement dans le pool gacha.

**Flux normal : supprimer une récompense**

1. L'administrateur sélectionne une récompense et confirme la suppression ;
2. La récompense est retirée du catalogue, n'affecte pas les cartes déjà obtenues par les joueurs, ça n'est pas une
   erreur, c'est pour créer du FOMO (https://en.wikipedia.org/wiki/Fear_of_missing_out) et un sentiment de rareté vis à
   vis de certaines cartes plus disponibles (mais toujours échangeable).
   On peut imaginer des cartes spéciales noël, ce genre de chose).

---

### 4.5 Gacha

~~~plantuml
@startuml
left to right direction
    skinparam backgroundColor #EEEBDC
:Utilisateur Steam: --> (Effectuer un tirage Gacha)
@enduml
~~~

#### 4.5.1 Effectuer un tirage Gacha

**Description** - Dépenser des points pour obtenir aléatoirement des cartes. Chaque carte a une rareté (commune, peu
commune, rare, épique, légendaire) et une finition (normale, holographique, foil, polychrome, négative). Les cartes
obtenues s'ajoutent à l'inventaire.

**Acteurs** - Utilisateur Steam

**Pré-conditions** - Authentifié + (Points >= le nmb de point nécessaire pour un lancer * nombre de tirages souhaités).

**Post-conditions** - Point diminué de X * nombre de tirages. Cartes ajoutées à l'inventaire.

**Contraintes non fonctionnelles** - Rareté et finition déterminées selon des probabilités configurables.

~~~plantuml
@startuml
    skinparam backgroundColor #EEEBDC
    skinparam ranksep 30
start
:accéder à la page Gacha;
:choisir le nombre de tirages (x);
:lancer le tirage;
if (score suffisant?) then (non)
    :afficher erreur point insuffisant;
    stop
else (oui)
endif
:déduire les points (x * tirages);
:tirer les cartes aléatoirement;
:ajouter les cartes à l'inventaire;
:afficher les cartes obtenues;
stop
@enduml
~~~

---

### 4.6 Inventaire et Level Up

~~~plantuml
@startuml
left to right direction
    skinparam backgroundColor #EEEBDC
:Utilisateur Steam: --> (Consulter son inventaire)
:Utilisateur Steam: --> (Convertir des cartes en XP)
@enduml
~~~

#### 4.6.1 Consulter son inventaire

**Description** - Affiche toutes les cartes collectées par l'utilisateur, triées par rareté (légendaire -> commune),
avec
leur finition et le nombre de copies. Les cartes en cours de vente sur le marché sont exclues.

**Pré-conditions** - Authentifié.

#### 4.6.2 Convertir des cartes en XP

**Description** - Détruire des cartes pour gagner de l'XP Bruine via l'interface "Steam Machine" (drap&drop).
L'XP gagnée dépend de la rareté et de la finition.

**Acteurs** - Utilisateur Steam

**Pré-conditions** - Authentifié. Posséder des cartes non vendues et non présentes dans le deck.

**Post-conditions** - Cartes sélectionnées supprimées de l'inventaire. XP totale augmentée.

**Contraintes non fonctionnelles** - Une carte dans le deck ou en vente ne peut pas être convertie.

- Formule XP : `r * f`
    - r : rareté
    - f : finition

~~~plantuml
@startuml
    skinparam backgroundColor #EEEBDC
    skinparam ranksep 30
start
:accéder à la page Level Up;
:glisser des cartes dans la Steam Machine;
:afficher l'aperçu XP;
:confirmer la conversion;
:supprimer les cartes sélectionnées;
:créditer l'XP;
:afficher le nouveau total XP;
stop
@enduml
~~~

---

### 4.7 Deck

~~~plantuml
@startuml
left to right direction
    skinparam backgroundColor #EEEBDC
:Utilisateur Steam: --> (Gérer son deck)
:Visiteur: --> (Consulter le deck d'un joueur)
:Utilisateur Steam: --> (Consulter le deck d'un joueur)
@enduml
~~~

#### 4.7.1 Gérer son deck

**Description** - Composer un deck de 10 cartes via glisser-déposer. Le deck accepte les doublons et les finitions
multiples d'une même carte. Les cartes du deck sont protégées : ni vendables sur le marché, ni convertibles en XP.

**Pré-conditions** - Authentifié.

**Contraintes non fonctionnelles** - Max 10 cartes. Doublons et finitions multiples autorisés. Cartes en vente exclues
de la sélection.

#### 4.7.2 Consulter le deck d'un joueur (widget)

**Description** - Page HTML autonome intégrable en tant qu'image sur tout site/markdown externe. Affiche :
avatar, nom du joueur, temps de jeu Steam, XP Bruine, cartes du deck avec effets visuels de finition (autant que
possible).

**Acteurs** - Visiteur | Utilisateur Steam

**Pré-conditions** - L'identifiant Steam correspond à un utilisateur inscrit sur Bruine.

**Contraintes non fonctionnelles** - Page* sans navbar/footer/....

---

### 4.8 Marché

~~~plantuml
@startuml
left to right direction
    skinparam backgroundColor #EEEBDC
:Utilisateur Steam: --> (Vendre une carte)
:Utilisateur Steam: --> (Acheter une carte)
:Utilisateur Steam: --> (Annuler sa mise en vente)
@enduml
~~~

#### 4.8.1 Vendre une carte

**Description** - Mettre en vente une carte de l'inventaire à un prix défini en points Bruine. Une carte présente dans
le deck ne peut pas être vendue.

**Pré-conditions** - Authentifié. Posséder une carte non présente dans le deck.

**Flux normal**

1. L'utilisateur sélectionne une carte et définit un prix ;
2. Le système vérifie que la carte n'est pas dans le deck ;
3. L'annonce est créée ; la carte disparaît de l'inventaire.

**Flux alternatif** - carte protégée par le deck, vente refusée, message d'erreur.

**Post-conditions** - Si carte dans le marché alors la carte est indisponible pour l'inventaire, le deck et le level up.

#### 4.8.2 Acheter une carte

**Description** - Acheter une carte mise en vente par un autre joueur en dépensant des points Bruine.

**Pré-conditions** - Authentifié. Points >= prix de l'annonce.

**Flux normal**

1. L'utilisateur sélectionne une annonce et confirme l'achat ;
2. Le système déduit le prix du point de l'acheteur et le crédite au vendeur ;
3. La carte est transférée à l'acheteur.

**Flux alternatif** - point insuffisant : bouton d'achat désactivé dans l'interface.

**Post-conditions** - Point acheteur diminué du prix. Point vendeur augmenté. Carte transférée à l'acheteur.

**Contraintes non fonctionnelles** - Un joueur ne peut pas acheter ses propres annonces. Un joueur peut annuler sa
propre annonce pour récupérer sa carte.

~~~plantuml
@startuml
    skinparam backgroundColor #EEEBDC
    skinparam ranksep 30
start
:accéder au marché;
switch (action?)
case (vendre)
    :sélectionner une carte et définir un prix;
    if (carte dans le deck?) then (oui)
        :afficher erreur;
        stop
    else (non)
    endif
    :créer l'annonce;
    :masquer la carte de l'inventaire;
case (acheter)
    :sélectionner une annonce;
    if (point suffisant?) then (non)
        :bouton désactivé;
        stop
    else (oui)
    endif
    :déduire le prix (acheteur);
    :créditer le prix (vendeur);
    :transférer la carte;
case (annuler)
    :retirer l'annonce;
    :restituer la carte à l'inventaire;
endswitch
stop
@enduml
~~~

---

### 4.9 Classement

~~~plantuml
@startuml
left to right direction
    skinparam backgroundColor #EEEBDC
:Visiteur: --> (Consulter le classement)
:Utilisateur Steam: --> (Consulter le classement)
@enduml
~~~

#### 4.9.1 Consulter le classement

**Description** - Affiche le classement des joueurs Bruine selon deux critères : temps de jeu total Steam ou XP Bruine.
Un podium est affiché en tête, suivi du reste du classement sous forme de tableau.

**Acteurs** - Visiteur | Utilisateur Steam

**Pré-conditions** - Aucune.

---

## 5. Annexes

### 5.1 Terminologie

| Terme                           | Définition                                                                                                                               |
|---------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| **Identifiant Steam (steamId)** | Identifiant unique d'un utilisateur Steam. Nombre de 17 chiffres commençant par `7656119`.                                               |
| **OpenID 2.0**                  | Protocole d'authentification délégué utilisé par Steam. Permet de vérifier l'identité sans que Bruine ne manipule le mot de passe Steam. |
| **Compte Bruine**               | Profil d'un utilisateur Steam enregistré dans Bruine, distinct du compte Steam lui-même.                                                 |
| **Point/Currency/Score**        | Points Bruine. Monnaie utilisée pour le gatcha et les achats sur le marché.                                                              |
| **XP Bruine (totalExperience)** | Points d'expérience gagnés en convertissant des cartes.                                                                                  |
| **Gacha**                       | Système de tirage aléatoire de cartes en échange de points Bruine. (https://en.wikipedia.org/wiki/Gacha_game)                            |
| **Rareté**                      | Niveau de rareté d'une carte : commune, peu commune, rare, épique, légendaire.                                                           |
| **Finition (ECardFinish)**      | Traitement visuel d'une carte : normale, holographique, foil, polychrome, négative.                                                      |
| **Deck**                        | Sélection de max 10 cartes d'un joueur, visible publiquement via widget intégrable.                                                      |
| **Widget deck**                 | Page HTML autonome intégrable en tant qu'image, affichant profil + deck.                                                                 |
| **Marché**                      | Place de marché permettant l'échange de cartes contre des points.                                                                        |
| **Steam Machine**               | Interface de la page d'xp permettant de glisser-déposer des cartes pour les convertir en XP.                                             |