# Projet *Bruine* : Tests

`./gradlew :bruine-app:test` — 86 tests, JUnit 5, profil `test` (H2 en mémoire, aucun appel réseau).

## admin.mfa.TotpGeneratorTest

- `matchesRfc6238ReferenceVector` : le code TOTP correspond au vecteur officiel de la RFC 6238.
- `generatedSecretIs32Base32Chars` : un secret généré fait 32 caractères Base32.
- `verifyAcceptsCurrentCodeAndRejectsWrongOne` : le code courant est accepté, un code faux refusé.
- `verifyToleratesPreviousWindowButRejectsFarDrift` : la fenêtre précédente est tolérée, une dérive de 5 fenêtres refusée.

## config.WebSecurityConfigTest

- `anonymousCanOpenTheHomePage` : l'accueil est public.
- `anonymousCanOpenTheLeaderboards` : les classements sont publics.
- `anonymousCanOpenTheLegalPages` : mentions légales et politique de confidentialité sont publiques.
- `anonymousCanLoadTheSiteImages` : les images sous `/img` sont publiques.
- `anonymousIsRedirectedToSteamLoginFromPlayerPages` : les pages joueur redirigent vers la connexion Steam.
- `anonymousJsonRequestGetsUnauthorizedInsteadOfARedirect` : une requête JSON anonyme reçoit un 401, pas une redirection.
- `anonymousIsRedirectedToAdminLoginFromAdminPages` : les pages admin redirigent vers le login admin.
- `anonymousCanOpenTheAdminLoginPage` : la page de login admin est accessible.

## config.ThymeleafSvgConfigTest

- `eachLayerIsAStandaloneSvg` (×4) : chaque calque SVG est autonome, sans Thymeleaf ni référence externe.
- `theLevelUpPageInlinesEveryLayer` : la page levelup inclut tous les calques avec leurs classes d'animation.
- `htmlModePreservesSvgAttributeCase` : la casse des attributs SVG (`viewBox`…) est préservée au rendu.

## steam.service.SteamServiceTest

- `anAssertionMeantForAnotherSiteIsRejected` : une assertion OpenID avec un autre `return_to` est refusée.
- `anAssertionFromAnotherProviderIsRejected` : une assertion d'un autre fournisseur que Steam est refusée.
- `aClaimedIdDifferentFromTheIdentityIsRejected` : `claimed_id` différent de `identity` est refusé.
- `anIdentityNotCoveredByTheSignatureIsRejected` : une identité non signée par Steam est refusée.

## steam.service.ScoreConcurrencyTest

- `simultaneousPurchasesNeverSpendMoreThanTheBuyerOwns` : 10 achats simultanés à 10 💧 avec 50 💧 donnent exactement 5 ventes.
- `simultaneousSpinsNeverSpendMoreThanThePlayerOwns` : 10 tirages simultanés à 10 💧 avec 50 💧 donnent exactement 5 tirages.

## card.CardFragmentTest

- `inventoryPageUsesSharedFragment` : l'inventaire rend la carte via le fragment partagé.
- `inventoryHidesCounterForASingleCopy` : un exemplaire unique n'affiche pas de compteur.
- `levelUpPageUsesSharedFragment` : la page levelup rend la carte via le fragment partagé.
- `deckEditorUsesSharedFragment` : l'éditeur de deck rend la carte via le fragment partagé.

## gacha.service.GachaServiceTest

- `eachPullCostsTenPoints` : chaque tirage coûte 10 💧.
- `pullsAreCountedInThePlayerTotal` : les tirages s'ajoutent au compteur du joueur.
- `spinningIsRefusedWhenTheScoreDoesNotCoverTheCost` : score insuffisant, tirage refusé sans débit.
- `spinningIsAllowedWhenTheScoreCoversTheCostExactly` : un score égal au coût suffit.
- `aPullCountBelowOneIsBroughtBackToOne` : moins d'un tirage est ramené à 1.
- `aPullCountAboveTenIsCappedAtTen` : plus de 10 tirages est plafonné à 10.
- `everyPullAddsOneCardToTheCollection` : chaque tirage ajoute une carte.
- `onlyARarityWithAWeightCanBeDrawn` : une rareté de poids nul ne sort jamais.
- `finishesWithoutAnyWeightFallBackToThePlainOne` : sans poids de finition, la finition est normale.
- `aConfigurationEntirelyAtZeroStillDraws` : une configuration à zéro tire quand même une commune.
- `aRarityWithoutAnyCardStillCostsThePull` : une rareté sans carte coûte le tirage sans rien ajouter.
- `aMissingConfigurationFallsBackToTheDefaults` : une configuration absente utilise les valeurs par défaut.

## level.service.LevelUpServiceTest

- `experienceIsTheRarityBaseWhenTheFinishIsPlain` : une carte simple rapporte la base de sa rareté.
- `experienceScalesWithTheFinishMultiplier` : la finition multiplie l'XP.
- `eachRarityHasItsOwnBase` : chaque rareté a sa propre base.
- `aMultiplierOfZeroStillPaysTheRarityBase` : un multiplicateur à 0 rapporte quand même la base.
- `aCardListedOnTheMarketIsNotDestroyed` : une carte en vente n'est pas convertie.
- `aCardThePlayerDoesNotOwnIsIgnored` : une carte non possédée ne rapporte rien.
- `anUnknownFinishNameIsIgnored` : une finition inconnue est ignorée.
- `severalRequestsAreAddedUpAndCreditedToThePlayer` : plusieurs conversions s'additionnent et sont créditées.
- `convertingNothingChangesNothing` : une conversion vide ne change rien.
- `theOfferedCardsCarryTheExperienceTheyWillPay` : la liste affiche l'XP réelle de chaque carte.

## deck.service.DeckServiceTest

- `savingKeepsTheChosenCards` : les cartes choisies sont enregistrées.
- `theDeckIsCreatedOnFirstSaveAndBelongsToItsOwner` : le deck est créé au premier enregistrement pour son propriétaire.
- `savingReplacesThePreviousContent` : un enregistrement remplace le contenu précédent.
- `savingWithoutAnyCardEmptiesTheDeck` : enregistrer sans carte vide le deck.
- `aCardBelongingToSomeoneElseIsRejected` : la carte d'un autre joueur est écartée.
- `aCardListedOnTheMarketIsRejected` : une carte en vente est écartée.
- `anUnknownCardIsIgnored` : une carte inexistante est ignorée.
- `theSameCardCannotBePlacedTwice` : un même exemplaire n'occupe qu'une place.
- `theDeckHoldsTenCardsAtMost` : le deck garde 10 cartes au maximum.
- `aPlayerWithoutADeckHoldsNoCard` : un joueur sans deck a un deck vide.

## deck.DeckSharingTest

- `theDeckIsServedAsAnSvgImage` : `/deck/{steamId}` renvoie une image SVG.
- `theDeckTemplateIsAValidSvgFile` : le template `deck.svg` est un SVG valide.
- `theProfileShowsTheDeckImageWithoutAnIframe` : le profil affiche l'image du deck, sans iframe.

## market.service.MarketServiceTest

- `sellingPublishesTheListingAtTheAskedPrice` : une vente publie l'annonce au prix demandé.
- `sellingRefusesAPriceBelowOne` : un prix inférieur à 1 est refusé.
- `sellingRefusesACardOwnedBySomeoneElse` : on ne vend pas la carte d'un autre.
- `sellingRefusesACardPlacedInTheDeck` : une carte du deck ne peut pas être vendue.
- `sellingAllowsAnotherCopyOfACardInTheDeck` : un doublon d'une carte du deck reste vendable.
- `buyingMovesThePointsAndHandsOverTheCard` : un achat transfère les points et la carte.
- `buyingRefusesYourOwnListing` : on n'achète pas sa propre annonce.
- `buyingRefusesWhenTheScoreIsTooLow` : score insuffisant, achat refusé sans aucun mouvement.
- `buyingAnUnknownListingIsRefused` : une annonce inexistante est refusée.
- `cancellingWithdrawsTheListing` : le vendeur peut retirer son annonce.
- `cancellingRefusesSomeoneElsesListing` : on ne retire pas l'annonce d'un autre.

## shop.model.ShopPackTest

- `theBonusCountsTowardsThePointsCredited` : les points bonus sont crédités.
- `withoutAPromoTheEffectivePriceIsTheCatalogPrice` : sans promo, le prix est le prix catalogue.
- `anActivePromoTakesItsPercentageOffThePrice` : une promo active réduit le prix.
- `aPromoWithoutDatesIsAlwaysRunning` : une promo sans dates est toujours active.
- `aPromoIsIgnoredBeforeItStarts` : une promo pas encore commencée est ignorée.
- `aPromoIsIgnoredOnceItIsOver` : une promo terminée est ignorée.
- `bothPricesAreFormattedWithTwoDecimals` : les prix sont affichés avec deux décimales.

## shop.service.ShopServiceTest

- `markingAPackAsPopularClearsTheFlagOnTheOthers` : un seul pack peut être populaire.
- `savingAnOrdinaryPackLeavesTheOthersAlone` : enregistrer un pack ordinaire ne touche pas aux autres.
- `thePacksAreListedInTheOrderChosenByTheAdministrator` : les packs suivent l'ordre choisi par l'admin.
- `aPaymentAlreadyCreditedIsNotCreditedAgain` : un paiement déjà crédité ne l'est pas deux fois.
- `anAbsentSessionIdentifierCreditsNothing` : sans identifiant de session, rien n'est crédité.
