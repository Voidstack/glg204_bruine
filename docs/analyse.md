# Projet *Bruine* : Analyse V 1.0

## 1. Table des matières

- [1. Table des matières](#1-table-des-matières)
- [2. Objectif du document](#2-objectif-du-document)
- [3. Sélection des use cases](#3-sélection-des-use-cases)
- [4. use case](#4-use-case)
    - [4.1. Groupe 1 : authentification](#41-groupe-1--authentification)
        - [4.1.1. Se connecter via Steam](#411-se-connecter-via-steam)
        - [4.1.2. Se déconnecter](#412-se-déconnecter)
    - [4.2. Groupe 2 : marché](#42-groupe-2--marché)
        - [4.2.1. Vendre une carte](#421-vendre-une-carte)
        - [4.2.2. Acheter une carte](#422-acheter-une-carte)
    - [4.3. Groupe 3 : système gacha](#43-groupe-3--système-gacha)
        - [4.3.1. Effectuer un tirage Gacha](#431-effectuer-un-tirage-gacha)
        - [4.3.2. Consulter son inventaire](#432-consulter-son-inventaire)
        - [4.3.3. Convertir des cartes en XP](#433-convertir-des-cartes-en-xp)
    - [4.4. Groupe 4 : deck](#44-groupe-4--deck)
        - [4.4.1. Gérer son deck](#441-gérer-son-deck)
- [5. Regroupement des classes](#5-regroupement-des-classes)
    - [5.1. Groupe domaine](#51-groupe-domaine)
    - [5.2. Groupe cycle de vie](#52-groupe-cycle-de-vie)
    - [5.3. Groupe service](#53-groupe-service)
    - [5.4. Groupe interface utilisateur et système](#54-groupe-interface-utilisateur-et-système)
- [6. Terminologie](#6-terminologie)

---

## 2. Objectif du document

Analyse fondée sur l'expression des besoins du projet **Bruine**. Le but est
d'établir la liste des classes métier nécessaires pour modéliser chaque use case, et de voir comment elles
seront manipulées par l'application.

---

## 3. Sélection des use cases

*On évalue selon les critères risques et pertinence, en partant du principe qu'on démarre par ce qui est
à haut risque et à grande pertinence.*

Pour les risques :

1. j'ai déjà fait ça ;
2. c'est un cas connu, d'autres l'ont fait ;
3. c'est un problème bien documenté ;
4. c'est un problème à creuser, mais des exemples similaires existent ;
5. c'est innovant.

Pour la pertinence :

1. son absence serait à peine remarquée ;
2. le système reste cohérent sans cette fonctionnalité ;
3. la majeure partie du système peut fonctionner ;
4. une partie du système peut fonctionner ;
5. le système ne fonctionnera pas sans cette fonctionnalité.

| use case                           | Risques | Pertinence | Prioritaire |
|------------------------------------|---------|------------|-------------|
| **Se connecter via Steam**         | 3       | 5          | oui         |
| Se déconnecter                     | 1       | 4          | oui         |
| **Vendre une carte**               | 3       | 5          | oui         |
| **Acheter une carte**              | 3       | 5          | oui         |
| **Effectuer un tirage Gacha**      | 2       | 5          | oui         |
| **Consulter son inventaire**       | 1       | 4          | oui         |
| **Convertir des cartes en XP**     | 2       | 5          | oui         |
| **Gérer son deck**                 | 2       | 5          | oui         |
| Annuler sa mise en vente           | 1       | 4          | non         |
| Consulter un profil Steam          | 3       | 3          | non         |
| Consulter le classement            | 1       | 3          | non         |
| Supprimer son compte Bruine        | 1       | 2          | non         |
| Gérer les comptes admin (CRUD)     | 1       | 3          | non         |
| Gérer les utilisateurs Steam       | 1       | 3          | non         |
| Gérer les récompenses Gacha (CRUD) | 1       | 3          | non         |

Les use cases non prioritaires sont soit des opérations CRUD triviales (administration), soit des vues en lecture
seule sans logique métier complexe.

Le cas **Se connecter via Steam** est traité en premier, c'est la porte d'entrée de toutes les
fonctionnalités, et l'intégration par OpenID 2.0 avec Steam est une nouvelle notion pour moi.

Les cas **Vendre** et **Acheter une carte** (risque 3) sont traités avant le gacha, malgré la dépendance logique
apparente (il faut des cartes pour les vendre). En analyse, l'ordre de traitement suit le risque, pas les
dépendances fonctionnelles. Ce sont les use cases qui introduisent les contraintes les plus structurantes sur le
modèle.

---

## 4. use case

### 4.1. Groupe 1 : authentification

#### 4.1.1. Se connecter via Steam

Un visiteur clique sur "Se connecter avec Steam". Il est redirigé vers Steam qui vérifie son identité (OpenID 2.0)
et renvoie un callback avec son `steamId`. Si c'est sa première connexion, un compte Bruine est créé automatiquement.

##### Classes candidates

~~~plantuml
@startuml
skin rose
hide empty members

class SteamUser <<entity>> {
  steamId : String
  username : String
  score : int
  totalExperience : long
  initialPlaytimeMinutes : Long
  currentPlaytimeMinutes : Long
}

class SteamController <<boundary>> {}
class SteamAuthenticationProvider <<control>> {}
class SteamService <<control>> {}
class UserRepository <<lifecycle>> {}
@enduml
~~~

##### Diagramme de séquence, cas nominal

~~~plantuml
@startuml
skin rose
actor Visiteur as v
boundary SteamController as ctrl
control SteamAuthenticationProvider as auth
control SteamService as svc
entity "u : SteamUser" as u
participant UserRepository as repo <<lifecycle>>
actor Steam as steam

v -> ctrl : GET /steam/login
ctrl --> v : redirect Steam OpenID

steam --> ctrl : GET /steam/login/redirect?openid.*

ctrl -> auth : authenticate(openidParams)
auth -> svc : verifyOpenIdSignature(openidParams)
svc --> auth : steamId

auth -> repo : findBySteamId(steamId)
repo --> auth : Optional<SteamUser>

alt premier connexion
  auth -> svc : getUserData(steamId)
  svc --> auth : profil Steam (username, avatarUrl, playtime)
  auth -> u : new SteamUser(steamId, username, playtime)
  auth -> repo : save(u)
else reconnexion
  auth -> repo : save(u) [update playtime]
end

auth --> ctrl : SteamUserPrincipal
ctrl --> v : redirect /
@enduml
~~~

##### Déroulement alternatif : signature OpenID invalide

~~~plantuml
@startuml
skin rose
actor Visiteur as v
boundary SteamController as ctrl
control SteamAuthenticationProvider as auth
control SteamService as svc

v -> ctrl : GET /steam/login/redirect?openid.*
ctrl -> auth : authenticate(openidParams)
auth -> svc : verifyOpenIdSignature(openidParams)
svc --> auth : échec vérification
auth --> ctrl : AuthenticationException
ctrl --> v : redirect /
@enduml
~~~

##### Classes consolidées

~~~plantuml
@startuml
skin rose
hide empty members

class SteamUser <<entity>> {
  steamId : String
  username : String
  score : int
  totalExperience : long
  initialPlaytimeMinutes : Long
  currentPlaytimeMinutes : Long
}

class SteamController <<boundary>> {
  login() : redirect
  loginRedirect(openidParams) : redirect
  logout() : redirect
}

class SteamAuthenticationProvider <<control>> {
  authenticate(openidParams) : SteamUserPrincipal
}

class SteamService <<control>> {
  verifyOpenIdSignature(openidParams) : String
  getUserData(steamId) : SteamUserData
}

class UserRepository <<lifecycle>> {
  findBySteamId(steamId : String) : Optional<SteamUser>
  save(u : SteamUser) : SteamUser
}

SteamController ..> SteamAuthenticationProvider
SteamAuthenticationProvider ..> SteamService
SteamAuthenticationProvider ..> UserRepository
SteamAuthenticationProvider ..> SteamUser
SteamUser <.. UserRepository
@enduml
~~~

---

#### 4.1.2. Se déconnecter

Un utilisateur Steam ferme sa session sans affecter une éventuelle session admin ouverte simultanément dans le même
navigateur.

La contrainte de coexistence des deux sessions dans la même session HTTP rend ce use case non trivial. Invalider
la session HTTP globale détruirait les deux contextes de sécurité.

##### Classes candidates

~~~plantuml
@startuml
skin rose
hide empty members

class SteamController <<boundary>> {}
class HttpSession <<boundary>> {}
@enduml
~~~

##### Diagramme de séquence

~~~plantuml
@startuml
skin rose
actor "Utilisateur Steam" as u
boundary SteamController as ctrl
boundary HttpSession as session

u -> ctrl : POST /steam/logout
ctrl -> session : removeAttribute("SPRING_SECURITY_CONTEXT")
ctrl --> u : redirect /
@enduml
~~~

**Remarque** : on n'appelle délibérément pas `session.invalidate()`. Cette décision découle de la contrainte
d'isolation des sessions. Elle sera documentée tout au long du développement.

##### Classes consolidées

~~~plantuml
@startuml
skin rose
hide empty members

class SteamController <<boundary>> {
  logout() : redirect
}

class HttpSession <<boundary>> {
  removeAttribute(key : String)
}

SteamController ..> HttpSession
@enduml
~~~

---

### 4.2. Groupe 2 : marché

Le marché est traité avant le gacha car il introduit les contraintes les plus structurantes sur le modèle :
`UserGachaReward` doit supporter un transfert de propriété, et la protection deck <-> marché impose une dépendance
inter-modules dès la conception du modèle.

#### 4.2.1. Vendre une carte

Le joueur met en vente une carte de son inventaire à un prix en points Bruine. Deux protections s'appliquent, la
carte doit lui appartenir et ne doit pas être présente dans son deck. Si les conditions sont remplies, une
`MarketListing` est créée.

##### Classes candidates

~~~plantuml
@startuml
skin rose
hide empty members

class MarketListing <<entity>> {
  price : int
  createdAt : LocalDateTime
}

class UserGachaReward <<entity>> {}
class SteamUser <<entity>> {}
class Deck <<entity>> {}

class MarketController <<boundary>> {}
class MarketService <<control>> {}
class DeckService <<control>> {}
class MarketListingRepository <<lifecycle>> {}
class UserGachaRewardRepository <<lifecycle>> {}
@enduml
~~~

##### Diagramme de séquence, cas nominal

~~~plantuml
@startuml
skin rose
actor "Utilisateur Steam" as u
boundary MarketController as ctrl
control MarketService as ms
control DeckService as ds
entity "listing : MarketListing" as listing
participant UserGachaRewardRepository as ugr <<lifecycle>>
participant MarketListingRepository as mlr <<lifecycle>>

u -> ctrl : POST /market/sell?ugrId=X&price=Y
ctrl -> ms : sell(user, ugrId, price)

ms -> ugr : findById(ugrId)
ugr --> ms : ugrObj

alt prix < 1
  ms --> ctrl : erreur prix invalide
else ugrObj.steamUser ≠ user
  ms --> ctrl : erreur carte non possédée
else
  ms -> ds : findDeckUgrIds(user.id)
  ds --> ms : deckUgrIds

  alt ugrId ∈ deckUgrIds
    ms --> ctrl : erreur carte dans le deck
  else OK
    ms -> listing : new MarketListing(user, ugrObj, price)
    ms -> mlr : save(listing)
    ms --> ctrl : succès
  end
end

ctrl --> u : redirect /market
@enduml
~~~

##### Classes consolidées

~~~plantuml
@startuml
skin rose
hide empty members

class MarketController <<boundary>> {
  page() : ModelAndView
  sell(ugrId : Long, price : int) : redirect
}

class MarketService <<control>> {
  sell(seller : SteamUser, ugrId : Long, price : int) : void
  findAllListings() : List<MarketListing>
  findMyListings(sellerId : Long) : List<MarketListing>
}

class DeckService <<control>> {
  findDeckUgrIds(userId : Long) : Set<Long>
}

class MarketListing <<entity>> {
  id : Long
  price : int
  createdAt : LocalDateTime
}

class MarketListingRepository <<lifecycle>> {
  save(listing : MarketListing) : MarketListing
  findAll() : List<MarketListing>
}

class UserGachaRewardRepository <<lifecycle>> {
  findById(id : Long) : Optional<UserGachaReward>
}

MarketController ..> MarketService
MarketService ..> MarketListingRepository
MarketService ..> UserGachaRewardRepository
MarketService ..> DeckService
MarketService ..> MarketListing
MarketListing --> SteamUser : vendeur >
MarketListing --> UserGachaReward : carte >
@enduml
~~~

---

#### 4.2.2. Acheter une carte

L'acheteur sélectionne une annonce disponible. Le système vérifie qu'il n'achète pas sa propre annonce et qu'il
dispose du score suffisant. Le score est transféré du vendeur vers l'acheteur, et la propriété de la
`UserGachaReward` est transférée. L'annonce est ensuite supprimée.

##### Classes candidates

~~~plantuml
@startuml
skin rose
hide empty members

class MarketListing <<entity>> {
  price : int
}

class SteamUser <<entity>> {
  score : int
}

class UserGachaReward <<entity>> {}

class MarketController <<boundary>> {}
class MarketService <<control>> {}
class MarketListingRepository <<lifecycle>> {}
class UserGachaRewardRepository <<lifecycle>> {}
class UserRepository <<lifecycle>> {}
@enduml
~~~

##### Diagramme de séquence, cas nominal

~~~plantuml
@startuml
skin rose
actor "Acheteur" as u
boundary MarketController as ctrl
control MarketService as ms
participant MarketListingRepository as mlr <<lifecycle>>
participant UserGachaRewardRepository as ugr <<lifecycle>>
participant UserRepository as ur <<lifecycle>>
entity "buyer : SteamUser" as buyer
entity "seller : SteamUser" as seller

u -> ctrl : POST /market/buy/{id}
ctrl -> ms : buy(acheteur, listingId)

ms -> mlr : findById(listingId)
mlr --> ms : listing

alt listing.seller == acheteur
  ms --> ctrl : erreur auto-achat interdit
else
  ms -> ur : findById(acheteur.id)
  ur --> ms : buyer
  ms -> ur : findById(listing.seller.id)
  ur --> ms : seller

  alt buyer.score < listing.price
    ms --> ctrl : erreur score insuffisant
  else transaction
    ms -> buyer : setScore(score - listing.price)
    ms -> seller : setScore(score + listing.price)
    ms -> ur : save(buyer)
    ms -> ur : save(seller)
    ms -> ugr : transferOwnership(listing.ugr.id, buyer)
    ms -> mlr : deleteById(listingId)
    ms --> ctrl : succès
  end
end

ctrl --> u : redirect /market
@enduml
~~~

##### Déroulement alternatif : annuler sa propre annonce

~~~plantuml
@startuml
skin rose
actor "Vendeur" as u
boundary MarketController as ctrl
control MarketService as ms
participant MarketListingRepository as mlr <<lifecycle>>

u -> ctrl : POST /market/cancel/{id}
ctrl -> ms : cancel(user, listingId)
ms -> mlr : findById(listingId)
mlr --> ms : listing

alt listing.seller ≠ user
  ms --> ctrl : erreur accès refusé
else
  ms -> mlr : deleteById(listingId)
  ms --> ctrl : succès
end

ctrl --> u : redirect /market
@enduml
~~~

##### Classes consolidées

~~~plantuml
@startuml
skin rose
hide empty members

class MarketController <<boundary>> {
  page() : ModelAndView
  buy(id : Long) : redirect
  cancel(id : Long) : redirect
}

class MarketService <<control>> {
  buy(buyer : SteamUser, listingId : Long) : void
  cancel(seller : SteamUser, listingId : Long) : void
}

class MarketListing <<entity>> {
  id : Long
  price : int
  createdAt : LocalDateTime
}

class SteamUser <<entity>> {
  score : int
  setScore(score : int)
}

class UserGachaReward <<entity>> {
  id : Long
}

class MarketListingRepository <<lifecycle>> {
  findById(id : Long) : Optional<MarketListing>
  deleteById(id : Long) : void
}

class UserGachaRewardRepository <<lifecycle>> {
  transferOwnership(ugrId : Long, newOwner : SteamUser) : void
}

class UserRepository <<lifecycle>> {
  findById(id : Long) : Optional<SteamUser>
  save(user : SteamUser) : SteamUser
}

MarketController ..> MarketService
MarketService ..> MarketListingRepository
MarketService ..> UserGachaRewardRepository
MarketService ..> UserRepository
MarketListing --> SteamUser : vendeur >
MarketListing --> UserGachaReward : carte >
@enduml
~~~

---

### 4.3. Groupe 3 : système gacha

#### 4.3.1. Effectuer un tirage Gacha

Un utilisateur authentifié dépense 10 points par tirage (1 à 10 tirages par requête). Le système tire aléatoirement
une rareté puis une finition selon des probabilités configurées, puis crée une `UserGachaReward` par carte obtenue.

##### Classes candidates

~~~plantuml
@startuml
skin rose
hide empty members

class SteamUser <<entity>> {
  score : int
  totalPulls : long
}

class GachaReward <<entity>> {
  emoji : String
  name : String
  rarity : String
  description : String
}

class UserGachaReward <<entity>> {
  finish : ECardFinish
}

enum ECardFinish {
  NORMAL
  HOLOGRAPHIC
  FOIL
  NEGATIVE
  POLYCHROME
}

class GachaConfig <<entity>> {
  rarityLegendary : int
  rarityEpic : int
  rarityRare : int
  rarityUncommon : int
  rarityCommon : int
  finishNormal : int
  finishHolographic : int
  finishFoil : int
  finishNegative : int
  finishPolychrome : int
}

class GachaController <<boundary>> {}
class GachaRewardService <<control>> {}
class GachaRewardRepository <<lifecycle>> {}
class UserGachaRewardRepository <<lifecycle>> {}
class GachaConfigRepository <<lifecycle>> {}
class UserRepository <<lifecycle>> {}
@enduml
~~~

##### Diagramme de séquence, cas nominal

~~~plantuml
@startuml
skin rose
actor "Utilisateur Steam" as u
boundary GachaController as ctrl
control GachaRewardService as grs
entity "user : SteamUser" as user
participant UserRepository as ur <<lifecycle>>
participant GachaConfigRepository as gcr <<lifecycle>>
participant UserGachaRewardRepository as ugr <<lifecycle>>

u -> ctrl : POST /gacha/spin?count=N

ctrl -> ur : findBySteamId(steamId)
ur --> ctrl : user

ctrl -> user : getScore()
user --> ctrl : score

alt score < N × 10
  ctrl --> u : erreur score insuffisant
else score suffisant
  ctrl -> user : setScore(score - N×10)
  ctrl -> user : setTotalPulls(totalPulls + N)
  ctrl -> ur : save(user)

  ctrl -> gcr : findById(1)
  gcr --> ctrl : cfg

  loop N fois
    ctrl -> ctrl : rollRarity(cfg) → rarity
    ctrl -> grs : findByRarity(rarity)
    grs --> ctrl : pool de GachaReward
    ctrl -> ctrl : pick(random, pool) → reward
    ctrl -> ctrl : rollFinish(cfg) → finish
    ctrl -> ugr : save(new UserGachaReward(user, reward, finish))
  end

  ctrl --> u : {results, newScore, totalCost}
end
@enduml
~~~

##### Classes consolidées

~~~plantuml
@startuml
skin rose
hide empty members

class GachaController <<boundary>> {
  page() : ModelAndView
  spin(count : int) : Map
  -rollRarity(cfg : GachaConfig) : String
  -rollFinish(cfg : GachaConfig) : ECardFinish
}

class GachaRewardService <<control>> {
  findByRarity(rarity : String) : List<GachaReward>
}

class GachaReward <<entity>> {
  id : Long
  emoji : String
  name : String
  rarity : String
  description : String
}

class UserGachaReward <<entity>> {
  id : Long
  finish : ECardFinish
}

enum ECardFinish {
  NORMAL
  HOLOGRAPHIC
  FOIL
  NEGATIVE
  POLYCHROME
}

class GachaConfig <<entity>> {
  rarityLegendary : int
  finishNormal : int
}

class GachaRewardRepository <<lifecycle>> {
  findByRarity(rarity : String) : List<GachaReward>
}

class UserGachaRewardRepository <<lifecycle>> {
  save(ugr : UserGachaReward) : UserGachaReward
}

class GachaConfigRepository <<lifecycle>> {
  findById(id : int) : Optional<GachaConfig>
}

class UserRepository <<lifecycle>> {
  findBySteamId(steamId : String) : Optional<SteamUser>
  save(u : SteamUser) : SteamUser
}

GachaController ..> GachaRewardService
GachaController ..> GachaConfigRepository
GachaController ..> UserRepository
GachaController ..> UserGachaRewardRepository
GachaRewardService ..> GachaRewardRepository
GachaRewardService ..> GachaReward
UserGachaReward --> ECardFinish
UserGachaReward --> GachaReward
@enduml
~~~

---

#### 4.3.2. Consulter son inventaire

L'utilisateur accède à la liste de toutes ses cartes, triées par rareté décroissante. Les cartes actuellement listées
sur le marché sont exclues de l'affichage.

Ce use case est essentiellement une lecture mais avec une particularité avec un filtrage sur les cartes en vente,
l'inventaire ne doit montrer que les cartes disponibles, pas celles engagées sur le marché.

##### Classes candidates

~~~plantuml
@startuml
skin rose
hide empty members

class InventoryController <<boundary>> {}
class UserGachaReward <<entity>> {}
class MarketListing <<entity>> {}
class UserGachaRewardRepository <<lifecycle>> {}
class MarketListingRepository <<lifecycle>> {}
@enduml
~~~

##### Diagramme de séquence

~~~plantuml
@startuml
skin rose
actor "Utilisateur Steam" as u
boundary InventoryController as ctrl
entity "user : SteamUser" as user
participant UserGachaRewardRepository as ugr <<lifecycle>>
participant MarketListingRepository as mlr <<lifecycle>>

u -> ctrl : GET /inventory
ctrl -> ugr : findAllBySteamUserId(user.id)
ugr --> ctrl : toutes les UGRs du joueur
ctrl -> mlr : findAllListedUgrIds()
mlr --> ctrl : listedIds
ctrl -> ctrl : filtrer UGRs non listées et trier par rareté
ctrl --> u : vue inventaire
@enduml
~~~

##### Classes consolidées

~~~plantuml
@startuml
skin rose
hide empty members

class InventoryController <<boundary>> {
  page() : ModelAndView
}

class UserGachaReward <<entity>> {
  id : Long
  finish : ECardFinish
}

class UserGachaRewardRepository <<lifecycle>> {
  findAllBySteamUserId(userId : Long) : List<UserGachaReward>
}

class MarketListingRepository <<lifecycle>> {
  findAllListedUgrIds() : Set<Long>
}

InventoryController ..> UserGachaRewardRepository
InventoryController ..> MarketListingRepository
InventoryController ..> UserGachaReward
@enduml
~~~

---

#### 4.3.3. Convertir des cartes en XP

L'utilisateur sélectionne des cartes via drag & drop dans la " Steam Machine " et confirme la conversion. Chaque carte
est détruite et une quantité d'XP calculée selon la formule `XP = base_rareté × multiplicateur_finish` est créditée.

Les cartes présentes dans le deck ou en vente sur le marché sont automatiquement exclues.

##### Classes candidates

~~~plantuml
@startuml
skin rose
hide empty members

class SteamUser <<entity>> {
  totalExperience : long
}

class UserGachaReward <<entity>> {
  finish : ECardFinish
}

class GachaReward <<entity>> {
  rarity : String
}

class LevelUpController <<boundary>> {}
class UserGachaRewardRepository <<lifecycle>> {}
class MarketListingRepository <<lifecycle>> {}
@enduml
~~~

##### Diagramme de séquence, cas nominal

~~~plantuml
@startuml
skin rose
actor "Utilisateur Steam" as u
boundary LevelUpController as ctrl
entity "user : SteamUser" as user
participant UserGachaRewardRepository as ugr <<lifecycle>>
participant MarketListingRepository as mlr <<lifecycle>>

u -> ctrl : POST /levelup/convert\n[{rewardId, finish}, ...]

ctrl -> ugr : findAllBySteamUserId(user.id)
ugr --> ctrl : toutes les UGRs

ctrl -> mlr : findAllListedUgrIds()
mlr --> ctrl : listedIds

loop pour chaque item (rewardId, finish)
  ctrl -> ctrl : trouver UGR correspondante
  alt UGR absente, listée sur le marché, ou dans le deck
    ctrl -> ctrl : ignorer cet item
  else UGR valide
    ctrl -> ctrl : computeXp(rarity, finish)
    ctrl -> ugr : delete(ugr)
    ctrl -> ctrl : totalXpGained += xp
  end
end

ctrl -> user : setTotalExperience(total + totalXpGained)
ctrl -> ur : save(user)
ctrl --> u : {xpGained, newTotalExperience}
@enduml
~~~

**Remarque** : le calcul de l'XP (de base) est le suivant :

| Rareté    | Base XP | Finish      | Multiplicateur |
|-----------|---------|-------------|----------------|
| Common    | 10      | NORMAL      | ×1             |
| Uncommon  | 25      | HOLOGRAPHIC | ×2             |
| Rare      | 75      | FOIL        | ×3             |
| Epic      | 200     | NEGATIVE    | ×4             |
| Legendary | 500     | POLYCHROME  | ×5             |

##### Classes consolidées

~~~plantuml
@startuml
skin rose
hide empty members

class LevelUpController <<boundary>> {
  page() : ModelAndView
  convert(items : List<ConvertRequest>) : ResponseEntity
  -computeXp(rarity : String, finish : ECardFinish) : int
}

class ConvertRequest <<boundary>> {
  rewardId : Long
  finish : String
}

class UserGachaReward <<entity>> {
  id : Long
  finish : ECardFinish
}

class GachaReward <<entity>> {
  rarity : String
}

class UserGachaRewardRepository <<lifecycle>> {
  findAllBySteamUserId(userId : Long) : List<UserGachaReward>
  delete(ugr : UserGachaReward) : void
}

class MarketListingRepository <<lifecycle>> {
  findAllListedUgrIds() : Set<Long>
}

LevelUpController ..> UserGachaRewardRepository
LevelUpController ..> MarketListingRepository
LevelUpController ..> ConvertRequest
UserGachaReward --> GachaReward
@enduml
~~~

---

### 4.4. Groupe 4 : deck

#### 4.4.1. Gérer son deck

L'utilisateur compose un deck de 0 à 10 cartes via drag & drop. Il peut mettre la même carte plusieurs fois
(doublons autorisés) et plusieurs finitions d'une même carte. La sauvegarde remplace le deck existant en entier.

Les cartes actuellement en vente sur le marché ne peuvent pas être ajoutées au deck.

##### Classes candidates

~~~plantuml
@startuml
skin rose
hide empty members

class Deck <<entity>> {
  id : Long
}

class UserGachaReward <<entity>> {
  id : Long
  finish : ECardFinish
}

class SteamUser <<entity>> {}

class DeckController <<boundary>> {}
class DeckService <<control>> {}
class DeckRepository <<lifecycle>> {}
class UserGachaRewardRepository <<lifecycle>> {}
class MarketListingRepository <<lifecycle>> {}
@enduml
~~~

##### Diagramme de séquence : affichage de la page

~~~plantuml
@startuml
skin rose
actor "Utilisateur Steam" as u
boundary DeckController as ctrl
control DeckService as ds
entity "deck : Deck" as deck
participant DeckRepository as dr <<lifecycle>>
participant UserGachaRewardRepository as ugr <<lifecycle>>
participant MarketListingRepository as mlr <<lifecycle>>

u -> ctrl : GET /deck
ctrl -> ds : findDeckByUser(user)
ds -> dr : findByUser(user)
dr --> ds : deck (ou null)

ctrl -> ugr : findAllBySteamUserId(user.id)
ugr --> ctrl : toutes les UGRs

ctrl -> mlr : findAllListedUgrIds()
mlr --> ctrl : listedIds

ctrl -> ctrl : filtrer UGRs disponibles (non listées)
ctrl --> u : vue éditeur de deck
@enduml
~~~

##### Diagramme de séquence : sauvegarder le deck

~~~plantuml
@startuml
skin rose
actor "Utilisateur Steam" as u
boundary DeckController as ctrl
control DeckService as ds
entity "deck : Deck" as deck
participant DeckRepository as dr <<lifecycle>>
participant UserGachaRewardRepository as ugr <<lifecycle>>

u -> ctrl : POST /deck (ugrIds[])
ctrl -> ds : saveDeck(user, ugrIds)
ds -> ugr : findAllById(ugrIds)
ugr --> ds : ugrs

ds -> ds : vérifier que chaque UGR appartient à user
ds -> ds : vérifier taille <= 10

ds -> dr : findByUser(user)
dr --> ds : deck existant (ou null)

alt deck existant
  ds -> deck : clearUgrs()
  ds -> deck : addAll(ugrs)
  ds -> dr : save(deck)
else pas de deck
  ds -> deck : new Deck(user, ugrs)
  ds -> dr : save(deck)
end

ctrl --> u : redirect /deck
@enduml
~~~

##### Déroulement alternatif : plus de 10 cartes

~~~plantuml
@startuml
skin rose
actor "Utilisateur Steam" as u
boundary DeckController as ctrl
control DeckService as ds

u -> ctrl : POST /deck (ugrIds[] avec plus de 10 ids)
ctrl -> ds : saveDeck(user, ugrIds)
ds -> ds : taille > 10
ds --> ctrl : erreur validation
ctrl --> u : message d'erreur
@enduml
~~~

##### Classes consolidées

~~~plantuml
@startuml
skin rose
hide empty members

class DeckController <<boundary>> {
  page() : ModelAndView
  saveDeck(ugrIds : List<Long>) : redirect
}

class DeckService <<control>> {
  findDeckByUser(user : SteamUser) : Optional<Deck>
  saveDeck(user : SteamUser, ugrIds : List<Long>) : void
  findDeckUgrIds(userId : Long) : Set<Long>
}

class Deck <<entity>> {
  id : Long
  clearUgrs()
  addAll(ugrs : List<UserGachaReward>)
}

class SteamUser <<entity>> {}

class UserGachaReward <<entity>> {
  id : Long
  finish : ECardFinish
}

class DeckRepository <<lifecycle>> {
  findByUser(user : SteamUser) : Optional<Deck>
  save(deck : Deck) : Deck
}

class UserGachaRewardRepository <<lifecycle>> {
  findAllById(ids : List<Long>) : List<UserGachaReward>
  findAllBySteamUserId(userId : Long) : List<UserGachaReward>
}

class MarketListingRepository <<lifecycle>> {
  findAllListedUgrIds() : Set<Long>
}

DeckController ..> DeckService
DeckService ..> DeckRepository
DeckService ..> UserGachaRewardRepository
DeckService ..> MarketListingRepository
DeckService ..> Deck
Deck --> "0..1" SteamUser
Deck --> "0..10" UserGachaReward
@enduml
~~~

---

## 5. Regroupement des classes

### 5.1. Groupe domaine

Ce groupe contient les entités métier du domaine Bruine. `SteamUser` est l'entité centrale ; toutes les autres y
sont reliées directement ou indirectement.

~~~plantuml
@startuml
skin rose
hide empty members

class SteamUser <<entity>> {
  id : Long
  steamId : String
  username : String
  score : int
  totalExperience : long
  totalPulls : long
  initialPlaytimeMinutes : Long
  currentPlaytimeMinutes : Long
  setScore(score : int)
  setTotalExperience(xp : long)
  setTotalPulls(pulls : long)
}

class GachaReward <<entity>> {
  id : Long
  emoji : String
  name : String
  rarity : String
  description : String
}

class UserGachaReward <<entity>> {
  id : Long
  finish : ECardFinish
}

enum ECardFinish {
  NORMAL
  HOLOGRAPHIC
  FOIL
  NEGATIVE
  POLYCHROME
}

class GachaConfig <<entity>> {
  rarityLegendary : int
  rarityEpic : int
  rarityRare : int
  rarityUncommon : int
  rarityCommon : int
  finishNormal : int
  finishHolographic : int
  finishFoil : int
  finishNegative : int
  finishPolychrome : int
}

class Deck <<entity>> {
  id : Long
  clearUgrs()
  addAll(ugrs : List<UserGachaReward>)
}

class MarketListing <<entity>> {
  id : Long
  price : int
  createdAt : LocalDateTime
}

SteamUser "1" -- "*" UserGachaReward : possède >
UserGachaReward "*" --> "1" GachaReward : est une >
UserGachaReward --> ECardFinish : finish >

Deck "0..1" --> "1" SteamUser : appartient à >
Deck --> "0..10" UserGachaReward : contient >

MarketListing "*" --> "1" SteamUser : vendeur >
MarketListing "1" --> "1" UserGachaReward : mise en vente >
@enduml
~~~

**Remarque** : `UserGachaReward` est l'entité pivot de l'application. Elle est produite par le gacha, consommée
par le level up, référencée par le deck, et mise en vente sur le marché. Chaque copie physique d'une carte est
une ligne distincte, ce qui permet de la référencer individuellement dans toutes ces opérations.

---

### 5.2. Groupe cycle de vie

~~~plantuml
@startuml
skin rose
hide empty members

class UserRepository <<lifecycle>> {
  findBySteamId(steamId : String) : Optional<SteamUser>
  findById(id : Long) : Optional<SteamUser>
  save(u : SteamUser) : SteamUser
  findAllOrderByPlaytime() : List<SteamUser>
  findAllOrderByXp() : List<SteamUser>
}

class GachaRewardRepository <<lifecycle>> {
  findByRarity(rarity : String) : List<GachaReward>
  findAll() : List<GachaReward>
  save(r : GachaReward) : GachaReward
  deleteById(id : Long) : void
}

class UserGachaRewardRepository <<lifecycle>> {
  save(ugr : UserGachaReward) : UserGachaReward
  findAllBySteamUserId(userId : Long) : List<UserGachaReward>
  findAllById(ids : List<Long>) : List<UserGachaReward>
  findById(id : Long) : Optional<UserGachaReward>
  delete(ugr : UserGachaReward) : void
  transferOwnership(ugrId : Long, newOwner : SteamUser) : void
}

class DeckRepository <<lifecycle>> {
  findByUser(user : SteamUser) : Optional<Deck>
  save(deck : Deck) : Deck
}

class MarketListingRepository <<lifecycle>> {
  save(listing : MarketListing) : MarketListing
  findAll() : List<MarketListing>
  findBySellerId(sellerId : Long) : List<MarketListing>
  findById(id : Long) : Optional<MarketListing>
  deleteById(id : Long) : void
  findAllListedUgrIds() : Set<Long>
}

class GachaConfigRepository <<lifecycle>> {
  findById(id : int) : Optional<GachaConfig>
}
@enduml
~~~

---

### 5.3. Groupe service

~~~plantuml
@startuml
skin rose
hide empty members

class SteamAuthenticationProvider <<control>> {
  authenticate(openidParams) : SteamUserPrincipal
}

class SteamService <<control>> {
  verifyOpenIdSignature(openidParams) : String
  getUserData(steamId : String) : SteamUserData
  getOwnedGames(steamId : String) : List<Game>
}

class GachaRewardService <<control>> {
  findByRarity(rarity : String) : List<GachaReward>
  findAll() : List<GachaReward>
  save(r : GachaReward) : GachaReward
  deleteById(id : Long) : void
}

class DeckService <<control>> {
  findDeckByUser(user : SteamUser) : Optional<Deck>
  saveDeck(user : SteamUser, ugrIds : List<Long>) : void
  findDeckUgrIds(userId : Long) : Set<Long>
}

class MarketService <<control>> {
  sell(seller : SteamUser, ugrId : Long, price : int) : void
  buy(buyer : SteamUser, listingId : Long) : void
  cancel(seller : SteamUser, listingId : Long) : void
  findAllListings() : List<MarketListing>
  findMyListings(sellerId : Long) : List<MarketListing>
}
@enduml
~~~

---

### 5.4. Groupe interface utilisateur et système

~~~plantuml
@startuml
skin rose
hide empty members

class SteamController <<boundary>> {
  login() : redirect
  loginRedirect(openidParams) : redirect
  logout() : redirect
  profile(steamId : String) : ModelAndView
  deleteAccount(steamId : String) : redirect
}

class MarketController <<boundary>> {
  page() : ModelAndView
  sell(ugrId : Long, price : int) : redirect
  buy(id : Long) : redirect
  cancel(id : Long) : redirect
}

class GachaController <<boundary>> {
  page() : ModelAndView
  spin(count : int) : Map
  -rollRarity(cfg : GachaConfig) : String
  -rollFinish(cfg : GachaConfig) : ECardFinish
}

class InventoryController <<boundary>> {
  page() : ModelAndView
}

class LevelUpController <<boundary>> {
  page() : ModelAndView
  convert(items : List<ConvertRequest>) : ResponseEntity
}

class DeckController <<boundary>> {
  page() : ModelAndView
  saveDeck(ugrIds : List<Long>) : redirect
  widget(steamId : String) : ModelAndView
}

class LeaderboardController <<boundary>> {
  playtimeLeaderboard() : ModelAndView
  xpLeaderboard() : ModelAndView
}

class AdminUserController <<boundary>> {
  page() : ModelAndView
  create(username, password) : redirect
  updatePassword(id, password) : redirect
  delete(id) : redirect
}

class AdminSteamUserController <<boundary>> {
  page() : ModelAndView
  delete(steamId) : redirect
}

class AdminGachaRewardController <<boundary>> {
  page() : ModelAndView
  create(emoji, name, rarity, description) : redirect
  update(id, ...) : redirect
  delete(id) : redirect
}
@enduml
~~~

---

## 6. Terminologie

| Terme          | Définition                                                                                                      |
|----------------|-----------------------------------------------------------------------------------------------------------------|
| **UGR/ugr**    | `UserGachaReward` : copie physique d'une carte appartenant à un joueur. Chaque tirage crée une ligne distincte. |
| **Score**      | Monnaie interne de Bruine (💧), gagnée via le temps de jeu Steam. Utilisée pour les tirages et le marché.       |
| **Finish**     | Finition d'une carte (effet visuel) : Normal, Holographique, Foil, Négatif, Polychrome.                         |
| **Rareté**     | Niveau de rareté d'une carte : Common, Uncommon, Rare, Epic, Legendary.                                         |
| **Deck**       | Sélection de 0 à 10 cartes exposées sur le profil public du joueur via un widget.                               |
| **Listing**    | Annonce de vente d'une carte sur le marché (`MarketListing`).                                                   |
| **XP**         | Expérience cumulée via la conversion de cartes (`totalExperience`).                                             |
| **steamId**    | Identifiant unique Steam au format `7656119XXXXXXXXXX` (17 chiffres).                                           |
| **OpenID 2.0** | Protocole d'authentification délégué utilisé par Steam. Bruine ne manipule aucun mot de passe Steam.            |
| **Boundary**   | Classe d'interface entre le système et un acteur (contrôleur HTTP, vue, API externe).                           |
| **Control**    | Classe de coordination métier (service, workflow). Porte la logique, délègue la persistance.                    |
| **Lifecycle**  | Classe responsable du cycle de vie des entités en base de données (repository).                                 |