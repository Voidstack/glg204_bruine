# Projet *Bruine* : Tests

`./gradlew :bruine-app:test` — 93 tests, JUnit 5, profil `test` (H2 en mémoire, aucun appel réseau).

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
- `anonymousIsRedirectedToAdminLoginFromAdminPages` : les pages admin redirigent vers le login admin.
- `stripeReachesTheWebhookWithoutSessionNorCsrfToken` : le webhook Stripe est joignable sans session ni jeton CSRF, seule la signature décide (signature invalide : 400, pas de redirection ni de 403).
- `anonymousCanOpenTheAdminLoginPage` : la page de login admin est accessible.
- `aValidSteamAssertionSignsThePlayerInWithAFreshSession` : une assertion acceptée (Steam simulé) connecte le joueur : contexte sauvegardé en session, identifiant de session changé.
- `aSteamAssertionRefusedLeadsToTheLoginFailurePage` : une assertion refusée renvoie vers `/steam/failed`.
- `steamLogoutKeepsTheAdminSignedIn` : la déconnexion Steam retire le contexte Steam et conserve le contexte admin de la même session.

## steam.service.SteamServiceTest

- `anIncompleteAssertionIsRejectedBeforeContactingSteam` : une assertion OpenID incomplète (signature vide) est refusée par la validation, sans appel à Steam.
- `anAssertionMeantForAnotherSiteIsRejected` : une assertion OpenID avec un autre `return_to` est refusée.
- `anAssertionFromAnotherProviderIsRejected` : une assertion d'un autre fournisseur que Steam est refusée.
- `aClaimedIdDifferentFromTheIdentityIsRejected` : `claimed_id` différent de `identity` est refusé.
- `anIdentityNotCoveredByTheSignatureIsRejected` : une identité non signée par Steam est refusée.

## steam.service.ScoreConcurrencyTest

- `simultaneousPurchasesNeverSpendMoreThanTheBuyerOwns` : 10 achats simultanés à 10 💧 avec 50 💧 donnent exactement 5 ventes.
- `simultaneousSpinsNeverSpendMoreThanThePlayerOwns` : 10 tirages simultanés à 10 💧 avec 50 💧 donnent exactement 5 tirages.

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
- `aMultiplierBelowOneIsSavedAsOne` : un multiplicateur d'XP saisi sous 1 est enregistré à 1.

## level.service.LevelUpServiceTest

- `experienceIsTheRarityBaseWhenTheFinishIsPlain` : une carte simple rapporte la base de sa rareté.
- `experienceScalesWithTheFinishMultiplier` : la finition multiplie l'XP.
- `eachRarityHasItsOwnBase` : chaque rareté a sa propre base.
- `aCardListedOnTheMarketIsNotDestroyed` : une carte en vente n'est pas convertie.
- `aCardPlacedInTheDeckIsNotDestroyed` : une carte posée dans le deck n'est pas convertie.
- `theFreeCopyIsConvertedWhileTheDeckCopyStays` : quand un exemplaire est dans le deck, c'est l'exemplaire libre de la même carte qui est converti.
- `aCardThePlayerDoesNotOwnIsIgnored` : une carte non possédée ne rapporte rien.
- `anUnknownFinishNameIsIgnored` : une finition inconnue est ignorée.
- `severalRequestsAreAddedUpAndCreditedToThePlayer` : plusieurs conversions s'additionnent et sont créditées.
- `convertingNothingChangesNothing` : une conversion vide ne change rien.
- `theOfferedCardsCarryTheExperienceTheyWillPay` : la liste affiche l'XP réelle de chaque carte.

## deck.service.DeckServiceTest

- `savingKeepsTheChosenCards` : les cartes choisies sont enregistrées.
- `theDeckKeepsTheOrderChosenByThePlayer` : les cartes sont relues dans l'ordre choisi, y compris après un réordonnancement écrit en base.
- `savingReplacesThePreviousContent` : un enregistrement remplace le contenu précédent.
- `savingWithoutAnyCardEmptiesTheDeck` : enregistrer sans carte vide le deck.
- `aCardBelongingToSomeoneElseIsRejected` : la carte d'un autre joueur est écartée.
- `aCardListedOnTheMarketIsRejected` : une carte en vente est écartée.
- `anUnknownCardIsIgnored` : une carte inexistante est ignorée.
- `theSameCardCannotBePlacedTwice` : un même exemplaire n'occupe qu'une place.
- `theDeckHoldsTenCardsAtMost` : le deck garde 10 cartes au maximum.
- `aNewDeckHoldsNoCard` : un deck qui vient d'être créé ne contient aucune carte.

## steam.service.SteamUserServiceTest

- `aNewPlayerGetsAnEmptyDeck` : la première connexion d'un joueur lui crée un deck vide.
- `aReturningPlayerKeepsHisSingleDeck` : les connexions suivantes ne créent pas de second deck.
- `aNewPlayerEarnsNothingForThePlaytimeHeAlreadyHad` : le temps de jeu connu à l'inscription ne rapporte aucun point.
- `eachFullHourPlayedSinceTheLastLoginPaysOnePull` : chaque heure complète jouée depuis la connexion précédente rapporte 10 💧, le prix d'un tirage.
- `minutesBelowAFullHourPayNothing` : des minutes qui ne complètent pas une heure ne rapportent rien.
- `anHourCompletedAcrossTwoLoginsIsCredited` : une heure commencée avant une connexion et terminée après est bien créditée.
- `theSameHoursAreNeverCreditedTwice` : une reconnexion sans nouvelle heure de jeu ne crédite rien.
- `aDecreasingPlaytimeCreditsNothingAndKeepsTheCounter` : un temps qui baisse ne crédite rien et ne fait pas reculer le compteur, seules les heures au-delà du maximum connu rapportent.
- `aPrivateProfileLeavesTheCountersUntouched` : un profil privé ne change ni le score ni le temps de jeu.
- `aProfileThatWasPrivateAtSignUpStartsCountingWhenItOpens` : un profil privé à l'inscription commence à compter à son ouverture, sans créditer le temps passé.

## deck.DeckSharingTest

- `theDeckIsServedAsAnSvgImage` : `/deck/{steamId}` renvoie une image SVG.

## market.service.MarketServiceTest

- `sellingPublishesTheListingAtTheAskedPrice` : une vente publie l'annonce au prix demandé.
- `sellingRefusesAPriceBelowOne` : un prix inférieur à 1 est refusé.
- `sellingRefusesACardOwnedBySomeoneElse` : on ne vend pas la carte d'un autre.
- `sellingRefusesACardPlacedInTheDeck` : une carte du deck ne peut pas être vendue.
- `sellingRefusesACardAlreadyOnSale` : une carte déjà en vente ne peut pas être remise en vente.
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

## shop.service.ShopServiceTest

- `markingAPackAsPopularClearsTheFlagOnTheOthers` : un seul pack peut être populaire.
- `savingAnOrdinaryPackLeavesTheOthersAlone` : enregistrer un pack ordinaire ne touche pas aux autres.
- `thePacksAreListedInTheOrderChosenByTheAdministrator` : les packs suivent l'ordre choisi par l'admin.
- `aPaymentAlreadyCreditedIsNotCreditedAgain` : un paiement déjà crédité ne l'est pas deux fois.
- `anAbsentSessionIdentifierCreditsNothing` : sans identifiant de session, rien n'est crédité.
- `aWebhookEventWithoutAValidSignatureIsRefused` : un événement de webhook mal signé est refusé.
- `theWebhookRefusesEveryEventWhenNoSecretIsConfigured` : sans secret de webhook configuré, tout événement est refusé, même signé.
- `aSignedPaidCheckoutEventGivesTheSessionToFulfill` : un événement signé de session payée donne la session à créditer.
- `aSignedEventThatIsNotAPaymentIsIgnored` : un événement signé qui n'est pas un paiement est ignoré.
