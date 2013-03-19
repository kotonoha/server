#Word recommendation system

*Purpose*: help user to select additional words that will help him
to further improve learning.

Based on a class of adding word it needs to create a ranked list of recommended items.

##Basic recommenders

###By structure (right now the single section)
* (KS) Kanji splitter: 一生懸命　→　一生　生懸　懸命　一生懸　生懸命
* (BK) Basic kuns: 動向　→　動く　向かう
* (BO) Basic ons: like previous, but words with on-reading, without additional kanji
* (2J) Simple jukugo: 食べる　→　食事　飲食

###After getting usage data
* (PR) Popular words that come with this one

##How to deal with different types of input

Recomemender should use weights to combine different basic recommenders
for different types of words. Weights should determine order of recommended
words that system proposes to user.

###3-length and 4-length jukugo
Priority: KS=100,BK=50,BO=50

###2-length jukugo
We try to find some other jukugo with kanji here
Priority: BK=100,BO=75,2J(1)=25,2J(2)=25
