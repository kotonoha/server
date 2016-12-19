let mod = angular.module('kotonoha', ['ui.popover4']);

const STATE_INIT = 0;
const STATE_QUESTION = 1;
const STATE_ANSWER = 2;
const STATE_NOCARDS = 3;
const STATE_MARK = 4;
const STATE_TIMEOUT = 5;
const STATE_READY = 6;

const EX_STATUS_INITIAL = -1000;

const keybindings = {
  49: 1, 122: 1, // 1 and z
  50: 2, 120: 2, // 2 and x
  51: 3, 99: 3, // 3 and c
  52: 4, 118: 4, // 4 and v
  53: 5, 98: 5 // 5 and b
};

mod.controller('RepeatController', ['$scope', '$http', 'RepeatBackend', function (scope, http, backend) {
  scope.state = STATE_INIT;

  scope.state_question = () => { return scope.state == STATE_QUESTION; };
  scope.state_answer = () => { return scope.state == STATE_ANSWER; };
  scope.state_mark = () => { return scope.state == STATE_MARK; };
  scope.state_ready = () => { return scope.state == STATE_READY; };
  scope.contentVisible = () => {
    let state = scope.state;
    return state == STATE_ANSWER || state == STATE_MARK || state == STATE_READY;
  };
  scope.card = null;

  scope.badExReasons = [
    {code: 1, text: "Invalid word"},
    {code: 2, text: "Invalid reading"},
    {code: 3, text: "Invalid markup"},
    {code: 4, text: "Bad example"},
    {code: 0, text: "Other"}
  ];

  let cardCache = [];
  let processed = [];
  var questionShown = new Date();
  var answerShown = new Date();
  var markAssigned = new Date();

  scope.nextCard = function () {
    if (cardCache.length == 0) {
      scope.state = STATE_NOCARDS;
      return;
    }

    if (scope.state == STATE_READY && scope.card) {
      let card = scope.card;
      let msg = {
        cmd: "nextTime",
        card: card.id,
        readyTime: new Date().getTime()
      };
      backend.toActor(msg);
    }

    let c = cardCache.shift();
    processed.push(c.id);
    if (processed.length > 100) {
      processed.shift();
    }
    scope.card = c;
    questionShown = new Date();
    scope.state = STATE_QUESTION;
    scope.exState = EX_STATUS_INITIAL; //initial state
  };

  scope.showAnswer = function () {
    scope.state = STATE_ANSWER;
    answerShown = new Date();
  };
  
  scope.mark = function (markVal, src) {
    let card = scope.card;
    markAssigned = new Date();
    let mark = {
      cmd: "mark",
      card: card.id,
      mark: markVal,
      remaining: cardCache.length,
      timestamp: markAssigned.getTime(),
      questionTime: questionShown.getTime(),
      answerTime: answerShown.getTime(),
      source: src,
      exId: card.rexIdx
    };
    backend.toActor(mark);
    scope.state = STATE_READY;
  };

  function doReport(cardId, exId, status) {
    let msg = {
      cmd: "report-ex",
      card: cardId,
      exId: exId,
      status: status
    };
    backend.toActor(msg);
    scope.exState = status;
  }
  
  scope.reportGood = function (cardId, exId) {
    doReport(cardId, exId, scope.exState == -1 ? EX_STATUS_INITIAL : -1);
  };
  
  scope.reportBad = function (cardId, exId, reason) {
    doReport(cardId, exId, reason); //-1 is good
  };

  scope.handleBadBtn = function (po, cardId, exId) {
    if (scope.exState < 0) {
      po.toggle();
    } else if (scope.exState >= 0) {
      doReport(cardId, exId, EX_STATUS_INITIAL);
    }
  };

  function processCards(cards) {
    for (let x of cards) {
      if (processed.indexOf(x.id) == -1) {
        cardCache.push(x);
      }
    }

    if ((scope.state == STATE_INIT || scope.state == STATE_NOCARDS) && cardCache.length > 0) {
      scope.nextCard();
    }
  }

  backend.onMessage(msg => {
    let cmd = msg.cmd || "";
    switch (cmd) {
      case "cards":
        scope.$apply(() => processCards(msg.data));
        break;
      case "rep-cnt":
        scope.$apply(() => {
          scope.curSession = msg.data.curSession;
          scope.today = msg.data.today;
        });
        break;
      case "timeout":
        scope.$apply(() => scope.state = STATE_TIMEOUT);
        break;
      default:
        console.log("unknown command", msg)
    }
  });

  $(document).on('keydown', (e) => {
    switch (scope.state) {
      case STATE_QUESTION:
        if (e.which == 32) { //handle space
          scope.$apply(() => scope.showAnswer());
          return false;
        }
        break;
      case STATE_ANSWER:
        let mark = keybindings[e.which];
        if (mark !== undefined) {
          scope.$apply(() => scope.mark(mark, 'kbd'));
          return false;
        }
        break;
      case STATE_READY:
        if (e.which == 32) {
          scope.$apply(() => scope.nextCard());
          return false;
        }
        break;
      default:
        return;
    }
  });
}]);
