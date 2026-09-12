# Projet *Software Tickets* : Analyse V 1.2

troisième version de l'analyse : 

- on y a inclus le système de délégation de tickets. 
- Par ailleurs, l'esquisse de travail sur le tableau de bord nous a conduit à revenir sur l'expression des besoins pour préciser les choses. La version actuelle de l'expression des besoins tient compte de ces révisions.
- le *use case* [Visualiser un ticket](#427-visualiser-un-ticket)  comportait une méthode décidemment trop « magique » : `TicketRepository.listTicketsForUser()` ; comme la décision de ce qu'il faut montrer à l'utilisateur est un point métier important, il nous a semblé que, d'une part, ce n'était pas à un objet de type « lifecycle » de gérer la chose, et que d'autre part, il fallait un peu détailler les choses.
- unification et rationalisation du vocabulaire :
  - remplacer `listYYY` par `findYYY` ;
  - remplacer `Documentation` par `ProgressReport`


## 1. Table des matières
- [1. Table des matières](#1-table-des-matières)
- [2. Objectif du document](#2-objectif-du-document)
- [3. Sélection des cas d'utilisation.](#3-sélection-des-cas-dutilisation)
- [4. Cas d’utilisation](#4-cas-dutilisation)
  - [4.1. Groupe 1 : gestion des utilisateur](#41-groupe-1--gestion-des-utilisateur)
    - [4.1.1. Créer un compte](#411-créer-un-compte)
    - [4.1.2. Se connecter](#412-se-connecter)
  - [4.2. Groupe 2 : gestion des tickets](#42-groupe-2--gestion-des-tickets)
    - [4.2.1. Créer un ticket](#421-créer-un-ticket)
    - [4.2.2. Valider un ticket](#422-valider-un-ticket)
    - [4.2.3. Créer un rapport d'avancement](#423-créer-un-rapport-davancement)
    - [4.2.4. Clore un ticket](#424-clore-un-ticket)
    - [4.2.5. Déléguer un ticket](#425-déléguer-un-ticket)
    - [4.2.6. Accepter ou refuser une délégation](#426-accepter-ou-refuser-une-délégation)
    - [4.2.7. Visualiser un ticket](#427-visualiser-un-ticket)
    - [4.2.8. Consulter le tableau de bord (Manager)](#428-consulter-le-tableau-de-bord-manager)
    - [4.2.9. Consulter le tableau de bord (Programmeur)](#429-consulter-le-tableau-de-bord-programmeur)
    - [4.2.10. Remarques finales](#4210-remarques-finales)
- [5. Regroupement des classes](#5-regroupement-des-classes)
  - [5.1. Groupe domaine](#51-groupe-domaine)
  - [5.2. Groupe domaine et cycle de vie](#52-groupe-domaine-et-cycle-de-vie)
  - [5.3. Groupe Service](#53-groupe-service)
  - [5.4. Groupe interface utilisateur et système](#54-groupe-interface-utilisateur-et-système)
- [6. Annexes](#6-annexes)
  - [6.1. Terminologie](#61-terminologie)
  - [6.2. Autre annexes](#62-autre-annexes)

## 2. Objectif du document

Ce document contient une analyse orientée objet fondée sur l'expression des besoins du projet "Software Tickets".
L'analyse suit le langage de modélisation UML et la méthodologie Arrington. Le but essentiel est d'établir la liste des classes métier nécessaires pour modéliser chaque cas d'utilisation, et de voir comment elles seront manipulées par l'application.



## 3. Sélection des cas d'utilisation.

*On commence par évaluer les use case selon les critères :*

- *risques*
- *pertinence*
- *compétence de l'équipe* (ça peut rentrer sous « risques » )

*Les risques sont par exemple* :
- *les performances* ;
- *la qualité de l'UI* ;
- *les difficultés de planning* ;
- *la difficulté de prendre en charge de nouvelles spécifications* ;
- ...
*Ils sont à considérer par rapport à l'expérience de l'équipe*.


*On démarre par ce qui est à haut risque et à grande pertinence.*

Pour les risques, on peut par exemple utiliser l'échelle :

1. notre équipe a déjà fait ça ;
2. une autre équipe de l'entreprise l'a déjà fait ;
3. c'est un problème bien documenté (outils, tutoriels) ;
4. c'est un problème à creuser, mais des exemples similaires existent ;
5. c'est innovant.

Pour la pertinence :

1. son absence serait à peine remarquée ;
2. le système reste cohérent sans cette fonctionnalité ;
3. la majeure partie du système peut fonctionner ;
4. une partie du système peut fonctionner ;
5. le système ne fonctionnera pas sans cette fonctionnalité.

En réalité, et en particulier pour les risques, il faudrait une analyse plus fine. Par exemple, ici, **la modélisation** des tickets et de leur historique, si elle n'est pas affreusement complexe, reste non triviale.

| Cas                                             | Risques | Pertinence | Prioritaire |
| ----------------------------------------------- | ------- | ---------- | ----------- |
| **Se connecter**                                | 1       | 3          | oui         |
| Créer Compte                                    | 1       | 3          | non         |
| **Créer un ticket**                             | aucun   | 5          | oui         |
| **Visualiser un ticket**                        | 3       | 5          | oui         |
| **Valider un ticket**                           | 2       | 5          | oui         |
| **Créer un rapport d'avancement**                        | 3       | 5          | oui         |
| **Clore un ticket**                             | 2       | 5          | oui         |
| **Déléguer un ticket**                              | 3       | 3          | oui         |
| **Accepter ou refuser un ticket**                   | 4       | 3          | oui         |
| Envoyer rappels                                 | 4       | 2          | non         |
| **Consulter le tableau de bord (manager)**     | 4       | 5          | oui         |
| **Consulter le tableau de bord (programmeur)** | 4       | 5          | oui         |
| Consulter le tableau de bord (client)           | 4       | 3          | non         |

Le tableau de bord peut être un peu complexe, en particulier à cause de la sélection des événements selon l'utilisateur.

Selon les critères retenus, on devra se concentrer sur

- la consultation du tableau de bord ;
- les divers éléments du cycle de vie du ticket.

Ceci dit, dans notre cas, le risque intervient moins que la pertinence. On n'a finalement conservé que les événements nécessaire au cycle de vie minimal d'un ticket.

Dans une première itération de ce document, on avait laissé de côté **Déléguer un ticket** et  **Accepter ou refuser un ticket** qui correspondent à des fonctionnalités non fondamentales du système. Mais ces *use cases* ont un impact sur les **visualisations** (**Visualiser un ticket** et les tableaux de bord). On a donc décidé de les inclure dans un second temps, pour bien comprendre ce que ça signifiait au niveau de l'analyse (et aussi parce qu'ils ont une modélisation potentiellement intéressante en conception).



## 4. Cas d’utilisation

### 4.1. Groupe 1 : gestion des utilisateur

#### 4.1.1. Créer un compte

Le texte mentionne les informations et les actions suivantes :
compte, login, mot de passe, type de compte, adresse mail, créer un compte, envoyer un mail.

Pour l'envoi de mail, j'ai volontairement choisi une modélisation assez peu réaliste, sans préjuger de ma conception ultérieure.


On ajoute mécaniquement :

- un *boundary* par couple acteur/use case ;
- un *control* par use case ;
- si on veut être précis, un *lifecycle* pour les entités qui en ont besoin.
 
On peut envisager le schéma suivant :

~~~plantuml
@startuml
skin rose

class User {
  login : String
  password : String
  group : TypeUser
  emailAddress
  contactUser()
}

enum TypeUser {
  CLIENT
  ADMIN
  PROGRAMMER
}

class CreateUserUI <<boundary>> {}
class CreateUserWorkflow <<control>> {}
class UserRepository <<lifecycle>> {}
class MailBoundary <<boundary>> {}

@enduml
~~~

- je choisirai (sauf si je l'oublie) d'utiliser le terme de *Repository* plutôt que celui de *Data Access Object* ; il est plus général, et est utilisé en *Domain Driven Design*.

##### Cas nominal

~~~plantuml
@startuml
skin rose
actor Administrator as a
boundary CreateUserUI as ui
control CreateUserWorkflow as wf
entity "u : User" as u
participant UserRepository as dao <<LifeCycle>>
boundary MailBoundary as mail
actor NewUser as newUser

a -> ui : setLogin()
a -> ui : setPassword1()
a -> ui : setPassword2()
a -> ui : setEmail()
a -> ui : setType()
ui -> ui : checkData()
ui -> wf : createUser()
wf -> dao : userExists(login)
dao --> wf : false
wf -> u : setLogin()
wf -> u : setPassword()
wf -> u : setEmail()
wf -> u : setType()
wf -> dao : save(u)
wf -> mail : sendMail()
mail -> newUser : mail
@enduml
~~~

##### Déroulement alternatif : données incorrectes

Honnêtement, ce diagramme est peu intéressant ; il n'apporte pas vraiment de nouvelle information


~~~plantuml
@startuml
skin rose
actor Administrator as a
boundary CreateUserUI as ui

a -> ui : setLogin()
a -> ui : setPassword1()
a -> ui : setPassword2()
a -> ui : setEmail()
a -> ui : setType()
ui -> ui : checkData() : false
ui --> a : message d'erreur
@enduml
~~~

##### Déroulement alternatif : le compte existe déjà

On pourrait éventuellement utiliser les possibilités d'UML, mais ça compliquerait le diagramme du cas nominal.

~~~plantuml
@startuml
skin rose
actor Administrator as a
boundary CreateUserUI as ui
control CreateUserWorkflow as wf
participant UserRepository as dao <<LifeCycle>>

a -> ui : setLogin()
a -> ui : setPassword1()
a -> ui : setPassword2()
a -> ui : setEmail()
a -> ui : setType()
ui -> ui : checkData()
ui -> wf : createUser()
wf -> dao : userExists(login)
dao --> wf : true
wf --> ui : erreur
ui --> a : erreur
@enduml
~~~

##### Classes consolidées

~~~plantuml
@startuml
skin rose

class User <<entity>> {
  login
  password
  type
  email
  setLogin()
  setPassword()
  setType()
  setEmail()
}

enum TypeUser <<entity>> {
  CLIENT
  MANAGER
  PROGRAMMER
  ADMINISTRATOR
}

User -> TypeUser

class CreateUserUI <<boundary>> {
  setLogin()
  setPassword1()
  setPassword2()
  setType()
  setEmail()
  checkData()
}

class CreateUserWorkflow <<control>> {
  createUser()
}

class UserRepository <<lifecycle>> {
  userExists(login) : boolean
  save(User)
}

class MailBoundary <<boundary>> {
  sendMail()
}

CreateUserUI ..> CreateUserWorkflow

CreateUserWorkflow ..> MailBoundary
CreateUserWorkflow ..> UserRepository
CreateUserWorkflow .> User
@enduml
~~~

##### Remarques
On a :

- déplacé la méthode d'expédition de mails ;
- modifié le champ qui indique le type d'utilisateur.

Des questions subsistent. Par exemple, pourra-t-on créer un nouvel administrateur ?

#### 4.1.2. Se connecter

La partie *diagramme de séquences* n'est pas très utile ici : on pressent déjà qu'on va faire gérer cette partie par le framework. Mais comme le but de ce fichier est pédagogique, on prend quand même la peine de décrire le *use case*.

##### Classes candidates

On a commme données et traitements : l'utilisateur, le login, le mot de passe, les *droits liés à son compte*. Ce qui donne :

~~~plantuml
@startuml
skin rose

class User {
  login
  password
  droits
}


class LoginUI <<boundary>> {}
class LoginWorkflow <<control>> {}
class UserRepository <<lifecycle>> {}
@enduml

@enduml
~~~

##### Diagramme de séquence (cas nominal)

~~~plantuml
@startuml
skin rose
actor user as a
boundary LoginUI as ui
control LoginWorkflow as wf
participant UserRepository as dao <<LifeCycle>>
entity "u: User" as u

a -> ui : setLogin()
a -> ui : setPassword()
a -> ui : login()
ui -> wf : login()
wf -> dao : findByLogin()
dao --> wf : u
wf -> u : checkPassword()
u --> wf : true
wf --> ui : ok
ui --> a : ok
@enduml
~~~

##### Déroulement alternatif : utilisateur inexistant

~~~plantuml
@startuml
skin rose
actor user as a
boundary LoginUI as ui
control LoginWorkflow as wf
participant UserRepository as dao <<LifeCycle>>
entity "u: User" as u

a -> ui : setLogin()
a -> ui : setPassword()
a -> ui : login()
ui -> wf : login()
wf -> dao : findByLogin()
dao --> wf : null
wf --> ui : erreur
ui --> a : erreur
@enduml
~~~

##### Déroulement alternatif : mot de passe incorrect

~~~plantuml
@startuml
skin rose
actor user as a
boundary LoginUI as ui
control LoginWorkflow as wf
participant UserRepository as dao <<LifeCycle>>
entity "u: User" as u

a -> ui : setLogin()
a -> ui : setPassword()
a -> ui : login()
ui -> wf : login()
wf -> dao : findByLogin()
dao --> wf : u
wf -> u : checkPassword()
u --> wf : false
wf --> ui : erreur
ui --> a : erreur
@enduml
~~~

##### Diagramme de classes consolidé

~~~plantuml
@startuml
skin rose

class User <<entity>> {
  login
  password
  checkPassword()
}



class LoginUI <<boundary>> {
  setLogin()
  setPassword()
  login()
}

LoginUI ..> LoginWorkflow
LoginWorkflow ..> UserRepository
LoginWorkflow .> User
User <.. UserRepository 


class LoginWorkflow <<control>> {
  login()
}

class UserRepository <<lifecycle>> {
  findByLogin() : User
}


@enduml

@enduml
~~~

### 4.2. Groupe 2 : gestion des tickets

#### 4.2.1. Créer un ticket

Si on procède mécaniquement, on a comme description :


- Le **Client** est connecté.
- il sélectionne le type de ticket (bug/amélioration) qu'il souhaite ;
- il donne un titre au ticket ;
- il écrit une description du ticket.
- le système vérifie que le ticket a un titre et une description
- le ticket est enregistré dans l'état "à valider"

##### Liste des objets candidats

~~~plantuml
@startuml
skin rose
hide empty members
class Client <<entity>> {}

class Ticket <<entity>> {
  title
  description
  ticketType
  state
}

class CreateTicketUI <<boundary>> {}

class CreateTicketWorkflow <<control>> {}
@enduml
~~~

##### Description des interactions entre objets (cas nominal)

~~~plantuml
@startuml
skin rose
actor client
boundary CreateTicketUI as ui
control CreateTicketWorkflow as wf
entity Ticket
entity "c: Client"
client -> ui : setTitle
client -> ui : setDescription
client -> ui : setTicketType
client -> ui : create()
ui -> ui : checkData()
alt checkData() == true
  ui -> wf : createTicket()
  wf -> Ticket : setTitle()
  wf -> Ticket : setDescription()
  wf -> Ticket : setType()
  wf -> Ticket : setState()
  wf -> Ticket : setClient(c)
  wf --> ui
  ui --> client : ok
else
  ui --> client : erreur
end
@enduml
~~~

**remarques** : 

1. dans ce diagramme, nous ne représentons pas le repository pour les tickets, en suivant ce que fait Arrington dans les cas simples. Pour être honnête, je le fais exprès pour montrer que c'est possible, mais j'aurais plutôt tendance  à ajouter le `TicketRepository` dans le schéma ;
2. de la même manière, j'ai fais apparaître « magiquement » dans le schéma l'objet `Client` qui correspond au client ; la récupération de cet objet pourrait se représenter ici, ou être laissée au moment de la conception. Pour être cohérent avec le premier point, c'est le choix que j'ai fait. à ce niveau-ci de notre travail, on n'a pas décidé, par exemple, si on séparait les fonctions de connexion et la représentation métier des acteurs dans le système. Nous sommes sur un cas très simple où la machinerie qu'on déploie est sur-dimensionnée. On pourrait de manière tout à fait légitime introduire aussi un `ClientRepository`

##### Classes consolidées

~~~plantuml
@startuml
skin rose
hide empty members

class Client <<entity>> {}

class Ticket <<entity>> {
  title
  description
  ticketType
  state  
  setTitle()
  setDescription()
  setClient()
  setState()
}

class CreateTicketUI <<boundary>> {
  setTitle()
  setDescription()
  setTicketType()
  checkData()  
}

class CreateTicketWorkflow <<control>> {
  createTicket()
}

CreateTicketUI ..> CreateTicketWorkflow

Ticket <. CreateTicketWorkflow 
Ticket --> Client

@enduml
~~~

#### 4.2.2. Valider un ticket

La description de ce cas comporte une information importante, la phrase :

> le message est enregistré dans **l’historique** du ticket

Dans un projet professionnel, réalisé pour un « vrai » client, je suppose que la notion d'historique sera explicitée de manière beaucoup plus précise que nous ne l'avons fait **lors de l'expression des besoins.** Comme c'est une exigence qui complexifie significativement le projet, il faudrait, à partir du moment où le besoin a été identifié, reprendre les *Use Cases* en y faisant préciser ce qu'il faut mettre dans l'historique.

Pour ce *use case* précis, je suis resté très simple, et j'ai considéré (pour l'instant) que la validation était un état qui n'arrivait qu'une seule fois dans la vie d'un ticket. Il n'a donc *a priori* pas besoin d'être historicisé (ça compliquerait d'ailleurs la gestion de l'historique). 

On pourra d'ailleurs imaginer que cet événement ne soit pas explicitement représenté dans la base de donnée comme un "événement", mais qu'il apparaisse comme tel dans la génération d'un historique. C'est potentiellement un cas où les DTO utilisés pour l'affichage et les représentations métiers internes pourraient diverger. Mais le problème concerne finalement surtout la **conception**.


##### Classes candidates

~~~plantuml
@startuml
skin rose
hide empty members

class Ticket <<entity>> {
  description
  client
  state
  validationDate
  validationRejectionMessage
}

class Programmer  <<entity>> {}

class Client  <<entity>> {}

Ticket ..> Client
Ticket .> Programmer

class ValidateTicketUI <<boundary>> {}

class ValidateTicketWorkflow <<control>> {}

class TicketRepository <<lifecycle>> {
  findTicketsToValidate()
}

class MailBoundary <<boundary>> {
  sendMail()
}

ValidateTicketUI ..> ValidateTicketWorkflow
ValidateTicketWorkflow ..> TicketRepository
ValidateTicketWorkflow ..> MailBoundary

ValidateTicketWorkflow ..> Ticket

@enduml
~~~

##### Diagrammes de séquence, cas nominal

~~~plantuml
@startuml
skin rose

actor Administrator as a0
boundary ValidateTicketUI as ui
control ValidateTicketWorkflow as wf
participant TicketRepository as dao <<lifecycle>>
entity "t: Ticket" as t
entity "p: Programmer" as p
boundary MailBoundary as mail
actor Client as a1

ui -> wf :  findTicketsToValidate()
wf -> dao :  findTicketsToValidate()
dao --> wf
wf --> ui
ui --> a0
a0 -> ui : selectTicket()
ui -> t : getDescription()
ui --> a0 : description
a0 -> ui : selectProgrammer()
ui -> wf : selectProgrammer()
wf -> t : setProgrammer(p)
a0 -> ui : validate()
ui -> wf : validate()
wf -> t : setDateValidation()
wf -> t : setState("validated")
wf -> mail : sendMail()
mail -> a1
@enduml
~~~


##### Diagrammes de séquence, refus de validation

~~~plantuml
@startuml
skin rose

actor Administrator as a0
boundary ValidateTicketUI as ui
control ValidateTicketWorkflow as wf
participant TicketRepository as dao <<lifecycle>>
entity "t: Ticket" as t
boundary MailBoundary as mail
actor Client as a1

ui -> wf :  findTicketsToValidate()
wf -> dao :  findTicketsToValidate()
dao --> wf
wf --> ui
ui --> a0
a0 -> ui : selectTicket()
ui -> t : getDescription()
ui --> a0 : description
a0 -> ui : rejectValidation(message)
ui -> wf : rejectValidation(message)
wf -> t : setValidationRejectionMessage()
wf -> t : setDateValidationRejection()
wf -> t : setState("closed")
wf -> mail : sendMail()
wf -> dao : update()
mail -> a1
@enduml
~~~

##### Classes consolidées

~~~plantuml
@startuml
skin rose
hide empty members


class Ticket <<entity>> {
  description
  client
  state
  validationDate
  validationRejectionMessage
  setProgrammer(p: Programmer)
  setDateValidation()
  setState()
  setValidationRejectionMessage()
}

class Programmer  <<entity>> {}

class Client  <<entity>> {}

Ticket ..> Client
Ticket .> Programmer

class ValidateTicketUI <<boundary>> {
  selectTicket()
  selectProgrammer()
  validate()
  rejectValidation(message)
}

class ValidateTicketWorkflow <<control>> {
  findTicketsToValidate()
  selectProgrammer()
  validate()
}

class TicketRepository <<lifecycle>> {
  findTicketsToValidate()
  update()
}

class MailBoundary <<boundary>> {
  sendMail()
}

ValidateTicketUI ..> ValidateTicketWorkflow
ValidateTicketWorkflow ..> TicketRepository
ValidateTicketWorkflow ..> MailBoundary

ValidateTicketWorkflow ..> Ticket

@enduml
~~~

#### 4.2.3. Créer un rapport d'avancement

##### Classes candidates

~~~plantuml
@startuml
skin rose
hide empty members

class Ticket <<entity>> {}

class ProgressReport <<entity>> {
  message
  date
}

Ticket --> "*" ProgressReport

class AddProgressReportUI <<boundary>> {}
class AddProgressReportWorkflow <<control>> {}
class TicketRepository <<lifecycle>> {}
@enduml
~~~


##### Diagramme de séquence : cas nominal

~~~plantuml
@startuml
skin rose

actor Programmer as a0
boundary AddProgressReportUI as ui
control AddProgressReportWorkflow as wf
entity "doc : ProgressReport" as doc
entity Ticket as t
participant TicketRepository as tdao

ui -> wf : listTicketsForProgrammer()
wf -> tdao : listTicketsForProgrammer()
tdao --> wf
wf --> ui
a0 -> ui : selectTicket()
a0 -> ui : setMessage()
a0 -> ui : addProgressReport()
ui -> wf : addProgressReport()
wf -> doc : setMessage()
wf -> doc : setDate()
wf -> t : addProgressReport(doc)
@enduml
~~~

##### Classes consolidées


~~~plantuml
@startuml
skin rose
hide empty members

class Ticket <<entity>> {
  addProgressReport(d: ProgressReport)
}

class ProgressReport <<entity>> {
  message
  date
  setMessage()
  setDate()
}

Ticket --> "*" ProgressReport

class AddProgressReportUI <<boundary>> {
  selectTicket()
  setMessage()
  addProgressReport()
}

class AddProgressReportWorkflow <<control>> {
  addProgressReport()
  listTicketsForProgrammer()
}

AddProgressReportUI ..> AddProgressReportWorkflow

class TicketRepository <<lifecycle>> {
  listTicketsForProgrammer()
}

AddProgressReportWorkflow ..> TicketRepository

@enduml
~~~
#### 4.2.4. Clore un ticket

##### Classes candidates

~~~plantuml
@startuml
  skin rose
  hide empty members
  class Ticket {
    closed : boolean
    closingDate
    close()
  }

  class TicketClosingUI <<boundary>> {}
  class TicketClosingWF <<control>> {}  
@enduml
~~~

##### Diagramme de séquence
~~~plantuml
  @startuml
  skin rose

  actor Programmer as a0
  boundary TicketClosingUI as ui
  control TicketClosingWF as wf
  entity "doc : ProgressReport" as doc
  entity Ticket as t
  participant TicketRepository as tdao

  note over a0, t: extension de "Créer un rapport d'avancement"
  a0 -> ui : closeTicket()
  ui -> wf : closeTicket()
  wf -> t : close()
  t -> t : closingDate = now  
@enduml
~~~

##### Classes consolidées

Note : on pourrait aussi envisager d'intégrer la clôture d'un ticket dans la phase de documentation de celui-ci.

~~~plantuml
@startuml
  skin rose
  hide empty members

  class Ticket {
    closed : boolean
    closingDate
    close()
  }

  class TicketClosingUI <<boundary>> {
    closeTicket()
  }

  class TicketClosingWF <<control>> {
    closeTicket()
  }  

  TicketClosingUI ..> TicketClosingWF
  TicketClosingWF .> Ticket
@enduml
~~~


#### 4.2.5. Déléguer un ticket

Un programmeur propose à un de ses collègue de prendre en charge un ticket

- 1. il choisit un de ses collègue ;
- 2. il remplit un formulaire qui contient :
  - une partie publique qui sera ajoutée à l'historique visible par le client ;
  - une partie privée à destination uniquement interne ; elle peut être vide
- 3. il valide

- la demande de délégation est enregistrée et ajoutée à l'historique ;
- elle sera visible sur le tableau de bord :
  - des deux programmeurs ;
  - des managers.

##### Classes Candidates

~~~plantuml
@startuml
skin rose
hide empty members

class Ticket {}
class Programmer {}
class Client {}
class DelegationMessage {
  publicText
  privateText
}

Ticket ..> Programmer
Ticket .> Client
Ticket <. DelegationMessage 
Programmer <.. DelegationMessage : < target
@enduml
~~~

##### Diagramme de séquence, cas nominal

~~~plantuml
@startuml
skin rose

actor Programmer as a
boundary DelegateTicketUI as ui
control DelegateWorkflow as wf
entity "t: Ticket" as t
entity DelegationMessage as m

a -> ui : choose programmer
a -> ui : public text
a -> ui : private text
a -> ui : submit()
ui -> wf : setTargetProgrammer(p)
ui -> wf : setPublicText()
ui -> wf : setPrivateText()
wf -> m : setTicket(t)
wf -> m : setTargetProgrammer(p)
wf -> m : setPublicText()
wf -> m : setPrivateText()
@enduml
~~~

##### Diagramme de classes consolidé


~~~plantuml
@startuml
skin rose
hide empty members

class Ticket {}
class Programmer {}

class DelegationMessage {
  publicText
  privateText
  setProgrammer(p)
  setTicket(t)
  setPublicText()
  setPrivateText()
}

Ticket ..> Programmer
Client <. Ticket 
Ticket <. DelegationMessage 
Programmer <.. DelegationMessage : < target

class DelegateTicketUI <<boundary>> {
  setProgrammer()
  setPublicText()
  setPrivateText()
  submit()
}

class DelegateTicketWorkflow <<control>> {
  setProgrammer(p: Programmer)
  setPublicText()
  setPrivateText()
}

DelegateTicketUI --> DelegateTicketWorkflow
DelegationMessage <. DelegateTicketWorkflow

Programmer <. DelegateTicketWorkflow
Ticket  <.. DelegateTicketWorkflow
@enduml
~~~

*note* : remarquez que le diagramme de séquences impose que le Workflow connaisse (sans qu'on sache trop comment pour l'instant) les programmeurs et le ticket.

#### 4.2.6. Accepter ou refuser une délégation


1. il visualise l'historique du ticket ;
2. il accepte de le prendre en charge ;
3. l'information est ajoutée dans l'historique du ticket


2.1 il refuse de le prendre en charge ;
1. l'information est ajoutée dans l'historique du ticket

- En cas d'acceptation :
  - le programmeur devient le gestionnaire du ticket ;
  - l'information est affichée sur les tableaux de bord du client, du programmeur qui a délégué le ticket, de celui qui l'a accepté, et du manager.
- En cas de refus : 
  - l'information est affichée sur les tableaux de bord du programmeur qui a délégué le ticket, de celui qui l'a refusé, et du manager.

##### Classes candidates

On commence par modéliser l'acceptation par des classes :

~~~plantuml
@startuml
skin rose
hide empty members
class DelegationMessage <<entity>> {}

class Ticket <<entity>> {}
class Acceptation {
  date
}

class Refusal {
  date
}

Acceptation .. DelegationMessage
Refusal .. DelegationMessage
DelegationMessage .. Programmer
DelegationMessage . Ticket
@enduml
~~~

mais à la réflexion, une demande de délégation correspond toujours à exactement une acceptation **ou** un refus, sauf quand elle n'a pas encore été traitée.

On décide donc de représenter cela plutôt par un **état** de la demande de délégation. Qui peut d'ailleurs changer de nom et devenir `DelegationRequest`.


~~~plantuml
@startuml
skin rose
hide empty members

class Programmer <<entity>> {}

class DelegationRequest <<entity>> {
  processingDate
}

enum DelegationRequestStatus {
  PENDING
  ACCEPTED
  REFUSED
}

class Ticket <<entity>> {}

DelegationRequest .. Programmer : > target
DelegationRequest . Ticket
DelegationRequestStatus . DelegationRequest : < status
class ProcessDelegationUI <<boundary>> {}

class ProcessDelegationWorkflow <<control>> {}

ProcessDelegationUI ..> ProcessDelegationWorkflow

@enduml
~~~

##### Cas nominal : acceptation d'une délégation

~~~plantuml
@startuml
skin rose

actor Programmer as a
boundary ProcessDelegationUI as ui
control ProcessDelegationWorkflow as wf
entity "t: Ticket" as t
entity DelegationRequest as rq

ui -> ui : displayTicket()
a -> ui : accept()
ui -> wf : accept()
wf -> rq : setProcessingDate(now)
wf -> rq : setStatus(ACCEPTED)
@enduml
~~~

##### Cas alternatif : refus d'une délégation


~~~plantuml
@startuml
skin rose

actor Programmer as a
boundary ProcessDelegationUI as ui
control ProcessDelegationWorkflow as wf
entity "t: Ticket" as t
entity DelegationRequest as rq

ui -> ui : displayTicket()
a -> ui : refuse()
ui -> wf : refuse()
wf -> rq : setProcessingDate(now)
wf -> rq : setStatus(REFUSED)
@enduml
~~~

##### Classes consolidées

~~~plantuml
@startuml
skin rose
hide empty members

class Programmer <<entity>> {}

class DelegationRequest <<entity>> {
  processingDate
  setStatus()
  setProcessingDate()
}

enum DelegationRequestStatus {
  PENDING
  ACCEPTED
  REFUSED
}

class Ticket <<entity>> {}

DelegationRequest .. Programmer : > target
DelegationRequest . Ticket
DelegationRequestStatus . DelegationRequest : < status

class ProcessDelegationUI <<boundary>> {
  displayTicket()
  accept()
  refuse()
}

class ProcessDelegationWorkflow <<control>> {
  accept()
  refuse()
}

Ticket <. ProcessDelegationUI 
DelegationRequest <. ProcessDelegationWorkflow


ProcessDelegationUI ..> ProcessDelegationWorkflow

@enduml
~~~

#### 4.2.7. Visualiser un ticket

La théorie, d'après **Arrington**, serait de commencer par les uses cases les plus risqués. Celui-ci est a priori l'un des plus complexes. Mais sa complexité provient de ce qu'il garde la trace des actions réalisées par les autres use cases. Comme rien ne nous oblige non plus à faire les choses bêtement, on le traite en dernier.


L'une des raisons qui nous ont poussé à inclure le *use case* **Déléguer un ticket** dans cette itération est que nous pensions qu'il conduirait naturellement à introduire un arbre d'héritage pour représenter les différents événements qui adviennent à un ticket. La position d'Arrington sur la modélisation métier lors de l'analyse est la suivante 

> *You should not spend much time seeking out inheritance hierarchies or determining the multiplicity of relationships. (...) If information seems obvious, then include it; otherwise, be very conservative as you spend time in analysis. Remember, the purpose of analysis is to discover and allo­cate responsibilities.*

Si l'usage de l'héritage s'impose de lui-même, il vaut mieux éviter de perdre du temps à essayer de le contourner. Sinon, le détail de l'organisation des classes peut attendre la conception.


##### Classes candidates
~~~plantuml
@startuml
skin rose
hide empty members

class Ticket <<entity>> {
  description
  client
  state
  validationOrRejectionDate  
  validationOrRejectionMessage
}

class Programmer  <<entity>> {}

class Client  <<entity>> {}

class TicketRepository <<lifecycle>> {} 

Ticket ..> Client
Ticket .> Programmer : > firstMaintainer

class ViewTicketUI <<boundary>> {}

class ViewTicketWorkflow <<control>> {}


class DelegationRequest <<entity>> {
  processingDate
  setStatus()
  setProcessingDate()
}

enum DelegationRequestStatus {
  PENDING
  ACCEPTED
  REFUSED
}

class Ticket <<entity>> {}

DelegationRequest .. Programmer : > target
DelegationRequest . Ticket
DelegationRequestStatus . DelegationRequest : < status


ViewTicketUI ..> ViewTicketWorkflow

ViewTicketWorkflow .> Ticket

ViewTicketWorkflow ..> TicketRepository

Ticket <.. TicketRepository 

@enduml
~~~

##### Diagramme de séquence nominal

Par rapport à la version 1.1, on a 

- clarifié la création de la liste des tickets.
- La méthode `sortFilterAndDisplay` était très floue ; on l'a déplacée dans le workflow (c'est du métier), et introduit une classe `TicketEvent` (qui sera un DTO en conception).


~~~plantuml
@startuml
 @startuml
  skin rose
  actor User as u
  boundary ViewTicketUI as ui
  control ViewTicketWorkflow as wf
  entity "t: Ticket" as t
  entity "doc : ProgressReport" as doc
  entity "req : DelegationRequest" as req
  participant TicketEventList as l
  participant TicketRepository as dao <<lifecycle>>
  
  ui -> wf : listTicketsForUser()
  alt user is manager
    wf -> dao : findAllTickets()
  else user is programmer
    wf -> dao : findTicketsForProgrammer()
    wf -> dao : findTickersWithDelegationRequestForProgrammer()
  else user is client
    wf -> dao : findTicketsForClient()
  end    
  wf --> ui
  u -> ui : selectTicket()  
  ui -> wf : getTicketsEvents(t)
  loop d: t.getProgressReport()
    wf -> l : add(d)
  end
  loop d: t.getDelegationRequests()
    wf -> l : add(d) if needed
  end  
  wf --> ui : l
  ui -> ui : displayEvents()  
@enduml
~~~

- All Delegation Requests are displayed for managers and programmers ;
- for client, only delegation requests which have been accepted are listed.

##### Diagramme de classes consolidé
~~~plantuml
@startuml
skin rose
hide empty members

together {
  class Ticket <<entity>> {
    title
    description
    creationDate
    evaluationMessage  
    evaluationDate  
    getProgressReports()
    getDelegationRequests()
  }

  enum TicketState {
    NEW
    OPEN
    REJECTED
    CLOSED  
  }

  note top of Ticket
  * creationDate est la date de la création du ticket par le client
  * evaluationDate est la date de traitement de la note par le manager.
  end note


  class ProgressReport <<entity>> {
    text
    date
    close
  }

  class Programmer  <<entity>> {}

  class Client  <<entity>> {}

  class DelegationRequest <<entity>> {
    processingDate
    setStatus()
    setProcessingDate()
  }

  enum DelegationRequestStatus {
    PENDING
    ACCEPTED
    REFUSED
  }


  Ticket -> "1" TicketState : > state
  Ticket *-- "*" ProgressReport
  Ticket --> Client : > client
  Ticket --> Programmer : > programmer
  
  DelegationRequest --> Programmer : > target
  DelegationRequest . Ticket
  DelegationRequestStatus . DelegationRequest : < status
}

class ViewTicketUI <<boundary>> {
  selectTicket()
  -sortAndDisplayEvents()
}

class ViewTicketWorkflow <<control>> {
  listTicketsForUser()
}

class TicketRepository <<lifecycle>> {
  findAllTickets()
  findTicketsForProgrammer()
  findTickersWithDelegationRequestForProgrammer()
  findTicketsForClient()
} 

ViewTicketUI ..> ViewTicketWorkflow

ViewTicketWorkflow .> Ticket

ViewTicketWorkflow ..> TicketRepository

Ticket <.. TicketRepository 

@enduml
~~~

**Note** : si on veut afficher toutes les informations sur un ticket, on veut probablement savoir quel programmeur est en charge à moment donné.

La modélisation proposée permet de le calculer ; on pourrait aussi envisager une représentation plus rendondante, où, par exemple, l'auteur d'un `ProgressReport` serait explicitement indiqué.


##### Remarques

- Quand il s'agit de *visualiser un ticket*, le programmeur ne voit-il que les tickets ouverts, ou tous ceux sur lesquels il a travaillé ?

#### 4.2.8. Consulter le tableau de bord (Manager)

- la liste des tickets à valider ;
- la liste des tickets récemment fermés.



##### Classes candidates

~~~plantuml
@startuml
skin rose
hide empty members

class Ticket <<entity>> {
  toValidate
  closed
  closingDate
}

note top of Ticket
  en fait, toValidate est simplement
  la propriété "state" de ticket
  quand elle est à NEW
end note
class ManagerDashboardUI <<boundary>> {}
class ManagerDashboardWorkflow <<control>> {}
class TicketEvent <<entity>> {}

ManagerDashboardUI ..> ManagerDashboardWorkflow

ManagerDashboardWorkflow .> Ticket

Ticket "1" .. "*" TicketEvent
@enduml
~~~

##### Diagramme de séquence, cas nominal

~~~plantuml
@startuml
 @startuml
  skin rose
  actor Manager as u
  boundary ManagerDashboardUI as ui
  control ManagerDashboardWorkflow as wf
  participant TicketRepository as dao <<lifecycle>>
  ui -> wf : findDashboardInfo()
  wf -> dao : findTicketsToValidate()
  wf -> dao : findRecentlyClosedTickets()
  wf --> ui
  ui -> ui : displayInfo()
@enduml
~~~

Les deux appels au `TicketRepository` font intervenir des informations qui proviennent des *uses cases*

- [**Créer un ticket**](#421-créer-un-ticket)
- et [**Clore un ticket**](#424-clore-un-ticket)

Elles permettent bien de savoir s'il faut ou non inclure tel ou tel ticket dans la liste., 

##### Classes consolidées


~~~plantuml
@startuml
skin rose
hide empty members

class Ticket <<entity>> {
  toValidate
  closed
  closingDate
}

note top of Ticket
  en fait, toValidate est simplement
  la propriété "state" de ticket
  quand elle est à NEW
end note

class ManagerDashboardUI <<boundary>> {
  displayEvents()
}


class ManagerDashboardWorkflow <<control>> {
  findDashboardInfo()
}

class TicketRepository <<lifecycle>> {
  findTicketsToValidate()
  findRecentlyClosedTickets()
}

class TicketEvent <<entity>> {}

ManagerDashboardUI ..> ManagerDashboardWorkflow
ManagerDashboardWorkflow ..> TicketRepository

ManagerDashboardWorkflow .> Ticket

Ticket "1" .. "*" TicketEvent
@enduml
~~~
#### 4.2.9. Consulter le tableau de bord (Programmeur)

- assignation d'un ticket par le manager ;
- demande de délégation par un autre développeur ;
- l'acceptation ou le refus de ses propres demandes de délégation
- ~~rappels~~ (plus tard)

##### Classes candidates

~~~plantuml
@startuml
skin rose
hide empty members
class Ticket <<entity>> {
  toValidate
}

class DelegationRequest {
  publicText
  privateText  
}

class Programmer {}

Ticket .> Programmer
Ticket .. DelegationRequest
DelegationRequest .> Programmer

class ProgrammerDashboardUI <<boundary>> {}
class ProgrammerDashboardWorkflow <<control>> {}

ProgrammerDashboardUI ..> ProgrammerDashboardWorkflow

ProgrammerDashboardWorkflow .> Ticket

@enduml
~~~

##### Diagramme de séquence, cas nominal

On liste :

- les nouveaux tickets par le manager ; en pratique, ceux sur lesquels le programmeur n'a encore rien fait ;
- demande de délégation par un autre développeur ;
- l'acceptation ou le refus de ses propres demandes de délégation
- ~~rappels~~ (plus tard)


~~~plantuml
@startuml
 @startuml
  skin rose
  actor Programmer as u
  boundary ProgrammerDashboardUI as ui
  control ProgrammerDashboardWorkflow as wf  
  entity "p: Programmer"
  participant TicketRepository as dao <<lifecycle>>
  participant DelegationRequestRepository as dao1 <<lifecycle>>

  u -> ui : display()
  ui -> wf : findDashboardInfoFor(p)
  wf -> dao : findTicketsForProgrammer(p)
  
  dao --> wf : tickets
  loop t: tickets
    wf -> t : t.isNewTicket()
  end loop

  wf -> dao1 : findNewDelegationRequestFor(p)
  wf -> dao : findNewDelegationRequestBy(p)
  wf --> ui
  ui -> ui : displayInfo()
@enduml
~~~


##### Classes consolidées
~~~plantuml
@startuml
skin rose
hide empty members

together {
  class Ticket <<entity>> {
    toValidate
    isNewTicket()
  }

  class DelegationRequest {
    publicText
    privateText
    setProgrammer(p)
    setTicket(t)
    setPublicText()
    setPrivateText()
  }

  class Programmer {}

  Ticket .> Programmer
  Ticket .. DelegationRequest
  DelegationRequest .> Programmer
}

together {
  class ProgrammerDashboardUI <<boundary>> {
    findDashboardInfoFor(p: Programmer)
    displayInfo()
  }

  class ProgrammerDashboardWorkflow <<control>> {
    findTicketsForProgrammer()
  }

  class TicketRepository <<lifecycle>> {
    findTicketsForProgrammer()
  }

  class DelegationRequestRepository <<lifecycle>> {
    findNewDelegationRequestFor(p: Programmer)
    findNewDelegationRequestBy(p: Programmer)
  }

  ProgrammerDashboardUI ..> ProgrammerDashboardWorkflow
  ProgrammerDashboardWorkflow ..> TicketRepository
  ProgrammerDashboardWorkflow ..> DelegationRequestRepository
}


ProgrammerDashboardWorkflow .> Ticket


@enduml
~~~

#### 4.2.10. Remarques finales

L'analyse est évidemment un processus itératif. En premier lieu, elle peut être reprise lors de l'évolution du logiciel ; mais elle suppose aussi plusieurs passages sur les *Use Cases* pour vérifier leur cohérence.

Par exemple, il n'est pas évident *a priori* qu'un `Ticket` nouvellement créé soit relié au `Client` qui l'a créé. Ce qui force le lien entre `Ticket` et `Client`, c'est le fait qu'on affiche, pour un client donné, tous les tickets qu'il a créés. D'ailleurs, en allant plus loin dans la modélisation (plutôt en conception), on *pourrait* se dire qu'on veut enregistrer les **événements** qui se produisent sur les tickets, et que la création est un événement comme un autre, et envisager de remplacer la liaison entre ticket et client par une liaison entre l'événement « création de ticket » et le client.

Un point intéressant, qui a surpris l'auteur, était qu'au départ, il me semblait que la modélisation du problème comme un ticket lié à une liste d'événements divers (utilisant l'héritage) s'imposait d'elle-même. Même si le problème est très simple, l'analyse montre que les événements ne sont pas tous égaux. Certains interviennent au plus une fois, à un moment précis de l'historique des tickets. D'autres sont répétables. 


Une autre question qui émerge pour la conception est celle de la non-redondance. Certaines informations sur un ticket sont déductible de son histoire. Dans ce cas, doit-on stocker les informations en question, ou les recalculer ? Par exemple, le programmeur en charge d'un ticket est, soit le premier programmeur nommé par le *manager*, soit le dernier programmeur à avoir accepté une *DelegationRequest*. Le calcul de cette information demande de parcourir l'historique du ticket. C'est l'approche logique. Cependant, on peut envisager de stocker ces informations. On reste pour l'instant sur l'approche minimaliste, en laissant la décision pour la phase de conception.

## 5. Regroupement des classes
### 5.1. Groupe domaine

On profite de la consolidation pour éliminer les synonymes.

~~~plantuml
@startuml
skin rose
hide empty members

together {
  class User <<entity>> {
    login
    password
    type
    email
    setLogin()
    setPassword()
    setType()
    setEmail()
    checkPassword()
  }

  enum TypeUser <<entity>> {
    CLIENT
    MANAGER
    PROGRAMMER
    ADMINISTRATOR
  }

  User -> TypeUser
}


class Ticket <<entity>> {
    title
    description
    creationDate    
    reviewMessage  
    reviewDate    
    closingDate
    closed : boolean
    close()
    isNewTicket()
    addProgressReport(d: ProgressReport)
    getProgressReports()
    getDelegationRequests()
    setTitle()
    setDescription()
    setClient()
    setState()
    setProgrammer(p: Programmer)
    setDateValidation()    
    setReviewMessage()
  }

  Ticket ..> TicketType : > type
  enum TicketType {
    BUG
    IMPROVEMENT
  }

  enum TicketState {
    NEW
    OPEN
    REJECTED
    CLOSED  
  }

  note top of Ticket
  * creationDate est la date de la création du ticket par le client
  * evaluationDate est la date de traitement de la note par le manager.
  end note


  class ProgressReport <<entity>> {
    date
    message
    setMessage()
    setDate()
  }

  class Programmer  <<entity>> {}

 
  class DelegationRequest <<entity>> {
    publicText
    privateText
    processingDate
    setStatus()
    setProcessingDate()
    setTarget(p : Programmer)
    setTicket(t)
    setPublicText()
    setPrivateText()
  }

  enum DelegationRequestStatus {
    PENDING
    ACCEPTED
    REFUSED
  }


  Ticket -> "1" TicketState : > state
  Ticket *-- "*" ProgressReport
  Ticket --> Client : > client
  Ticket --> Programmer : > firstMaintainer
  
  DelegationRequest --> Programmer : > target
  DelegationRequest . Ticket
  DelegationRequestStatus . DelegationRequest : < status
@enduml
~~~



### 5.2. Groupe domaine et cycle de vie



~~~plantuml
@startuml
skin rose

class UserRepository <<lifecycle>> {
  findByLogin() : User
  save(User)
  userExists(login) : boolean
}

class TicketRepository <<lifecycle>> {
  findAllTickets()
  findTicketsForProgrammer(p: Programmer)
  findTicketsToValidate()
  findRecentlyClosedTickets()
  findTickersWithDelegationRequestForProgrammer()
  findTicketsForClient()
  update()
}

note left of TicketRepository::findRecentlyClosedTickets
  il faudrait être
  plus explicite sur le sens de 
  « recently closed » 
end note

note left of TicketRepository::findTicketsForUser
  cette méthode n'est pas à sa place ici : 
  elle est liée au métier
  (que montre-t-on à quel utilisateur ?)
end note



  class DelegationRequestRepository <<lifecycle>> {
    findNewDelegationRequestFor(p: Programmer)
    findNewDelegationRequestBy(p: Programmer)
  }

@enduml
~~~

### 5.3. Groupe Service

~~~plantuml
@startuml
skin rose

  class CreateUserWorkflow <<control>> {
    createUser()
  }


  class LoginWorkflow <<control>> {
    login()
  }


  class CreateTicketWorkflow <<control>> {
    createTicket()
  }

  class ValidateTicketWorkflow <<control>> {
    findTicketsToValidate()
    selectProgrammer()
    validate()
  }


  class AddProgressReportWorkflow <<control>> {
    AddProgressReport()
    listTicketsForProgrammer()
  }


  class TicketClosingWF <<control>> {
    closeTicket()
  }  

  class DelegateTicketWorkflow <<control>> {
    setProgrammer(p: Programmer)
    setPublicText()
    setPrivateText()
  }

  class ProcessDelegationWorkflow <<control>> {
    accept()
    refuse()
  }


  class ViewTicketWorkflow <<control>> {
    listTicketsForUser()
  }

  class ManagerDashboardWorkflow <<control>> {
    findDashboardInfo()
  }


  class ProgrammerDashboardWorkflow <<control>> {
    findTicketsForProgrammer()
  }
@enduml
~~~





### 5.4. Groupe interface utilisateur et système



~~~plantuml
@startuml
skin rose

  class MailBoundary <<boundary>> {
    sendMail()
  }

  class CreateUserUI <<boundary>> {
    setLogin()
    setPassword1()
    setPassword2()
    setType()
    setEmail()
    checkData()
  }


  class LoginUI <<boundary>> {
    setLogin()
    setPassword()
    login()
  }


  class CreateTicketUI <<boundary>> {
    setTitle()
    setDescription()
    setTicketType()
    checkData()  
  }


  class ValidateTicketUI <<boundary>> {
    selectTicket()
    selectProgrammer()
    validate()
    rejectValidation(message)
  }


  class AddProgressReportUI <<boundary>> {
    selectTicket()
    setMessage()
    AddProgressReport()
  }


  class TicketClosingUI <<boundary>> {
    closeTicket()
  }


  class DelegateTicketUI <<boundary>> {
    setProgrammer()
    setPublicText()
    setPrivateText()
    submit()
  }


  class ProcessDelegationUI <<boundary>> {
    displayTicket()
    accept()
    refuse()
  }


  class ViewTicketUI <<boundary>> {
    selectTicket()
    -sortAndDisplayEvents()
  }


  class ManagerDashboardUI <<boundary>> {
    displayEvents()
  }


  class ProgrammerDashboardUI <<boundary>> {
    findDashboardInfoFor(p: Programmer)
    displayInfo()
  }
@enduml
~~~

## 6. Annexes
### 6.1. Terminologie

review
: évaluation d'un nouveau ticket par un manager

### 6.2. Autre annexes


