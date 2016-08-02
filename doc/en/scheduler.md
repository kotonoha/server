#Card scheduler specification

The heart of every SRS is a scheduler. It decides when should be the next repetition of
a card, in which order new cards should be displayed and so on.

##Glossary

* Old card - a card that has at least one mark.
* New card - a card that doesn't have even one mark.
* Good card - an old card that has 4 or 5 as a last mark.
* Bad card - an old card that has 1, 2 or 3 as a last mark.
* Repetition point - a timestamp that marks a point in time, when the card should be reviewed to get most out of
spacing effect.
* Ready card - an old card with its repetition point in the past.

##Requirements
There are 3 main lists of cards for a review.

* A list of ready cards. (Good and remembered cards)
* A list of bad cards. (Forgotten or not yet remembered cards)
* A list of new cards.

There should be 2 levels of schedulers.

* Three level-2 schedulers that govern three lists of new cards (SNew, SBad, SRdy)
* One level-1 scheduler that combines lists from lvl-2 schedulers to generate a list of cards for users.

##Level-2 schedulers

There are three lvl-2 schedulers. Schedulers for bad cards and ready cards are very simple,
but scheduler for new cards should take tag priorities and limits into account for constructing own list.

###Ready card scheduler

This scheduler constructs list of ready cards. Most of the time it should just order cards by repetition point,
but in case of user not reviewing cards for some days, scheduler should give some bias to cards that are near repetition
point too.

###Bad card scheduler

This scheduler constructs list of cards which had last mark 1, 2 or 3. User should have reviewed that card some time
away, to create some spaces even beetween bad repetitions.

###New card scheduler

This scheduler constructs a list of new cards. Cards are being prioritized by their tags as maximum priority of 
a tag that belong to card's word. Priorities come with limits, for example there shouldn't be more than
X cards with certain tag within a some time period.

Priorities by themself shouldn't be determenistic as ordering. If a card has a higher priority than another should mean
higher probability of that card appearing in a list.

##Level-1 scheduler

High-level scheduler should take output of low-level schedulers and create a single list of cards which contains
cards that will be presented to a user for a review in order they appear in the list. Main responsibility of this
scheduler will be a maintenance of a best possible form of the global card repetition forecast for a user.

Here is an example of such forecast.

![Learning forecast example](http://i.imgur.com/jKT0btP.png)

On 0 there are number of cards that have repetition points in 24 hours from now, 1 - from 24 to 48 hours, etc.
Graph can be divided in 2 areas that conform to one of two types of old cards in a learning process.

1. A certain ammount of bad cards or fresh good cards that aren't scheduled after a long spacing interval. Young cards.
2. An almost constant stream of good cards that were scheduled after pretty long spacing interval. Mature cards.

Cards become mature after a chain of repetitions with good marks.

###Operation modes

There is a number of situations which can be possible with learning. Scheduler should be able to cope with them all.

* Initial period - system wouldn't have enough data to do anything. It should end with 1000 card reviews or
14 days with reviews whatever comes first.
* Ready card starvation - when there is a low number of ready cards at the moment.
* New card starvation - same as above, but with new cards.
* Total starvation - ready and new cards starvation.
* After-rest period - user took some 'rest' from learning and decided to learn something again. There will be a great
 number of ready cards, more than user should be able to handle in one try.
* Normal workflow - none of above, the best condition for work, with everyday learning it should be the most frequent
 operating mode.

###Normal workflow

In the normal workflow scheduler will mix ready with bad cards in order to empty both lists simultaniously.
For the new cards, scheduler should decide their rate taking in concideration current repetition forecast and
average repetition numbers and marks for the last two weeks.

###Ready card starvation

Scheduler will move here usually from normal mode when user will drain ready and bad cards. In addition to new cards
scheduler should add to output list old cards that were scheduled long time ago and have both large
passed-time-from-repetition to interval-time rates and large interval lengths (more than two months).

###New card starvation

Scheduler should select cards like in normal mode, but instead of using new cards it should use additional cards
as described in ready card starvation mode.

###Total starvation

Scheduler should select cards as selected in ready card starvation mode, but it should stop selecting cards at all
when there are no cards that have repetition points in 2 weeks from now.

###After-rest period

Criteria: ready cards + bad cards > 2.5 * average learning count

The special thing about this mode that there is a lot of ready cards. Scheduler should consume list of ready cards in
two ways. The first on is a normal way: from a card that has the most distant repetition point from now. The second one
should select cards with short intervals that have repetition points near present moment. This way will allow us to
exploit spacing effect the most for such cards for the expense of cards with longer intervals being reviewed later than
they should have been.
