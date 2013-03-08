module = angular.module('kotonoha', ['collapsable', 'taglist', 'ui.popover2'])

resolver = (o) ->
  empty =
    writing: -> o.item.writing
    reading: -> o.item.reading
    meaning: -> o.item.meaning
    type: 'empty'
  good =
    writing: -> o.item.writing
    reading: -> o.dic[0].reading
    meaning: ->
      dic = o.dic[0]
      if (dic.meaning.length > 0) then dic.meaning[0] else ""
    type: 'good'
  bad =
    writing: -> o.item.writing
    reading: -> o.present[0].reading
    meaning: ->
      m = o.present[0].meaning
      if (m.length > 0) then m[0]
      else ""
    type: 'bad'
  if o.present.length != 0
    bad
  else if o.dic.length != 0
    good
  else empty


module.directive 'candidate', ($compile) ->
  restrict: 'A'
  link: (scope, elem, attrs) ->
    c = scope.c
    r = resolver(c)
    cl = 'cand-' + r.type
    elem.addClass(cl)
    scope.writing = r.writing()
    scope.reading = r.reading()
    scope.meaning = r.meaning()
    scope.type = r.type
#if (r.type != 'empty')
#  elem.click -> elem.dropdown('toggle')


window.AddWord = ($scope, AddWordActor) ->
  act = AddWordActor

  recieve = (msg) ->
    cmd = msg.cmd
    data = msg.data
    fn = cmds[cmd]
    if fn? then fn(data) else console.error("can't handle", msg)

  act.callback = recieve

  executing = false
  next = null

  update = (data) ->
    executing = false
    if (next != null)
      n = next
      next = null
      publish_update(n)
    $scope.$apply -> $scope.candidates = data

  cmds =
    update: update


  publish_update = (words) ->
    if executing
      next = words
      return
    executing = true
    act.toActor { cmd: "publish", data: words }


  $scope.tagdata = []
  $scope.words = ""
  $scope.candidates = []
  $scope.tagOps = []

  $scope.$watch "words", (newval, oldval) ->
    publish_update(newval)

  $scope.penaltize = (c) ->
    act.toActor { cmd: "penaltize", data: c }

  $scope.saveall = ->
    act.toActor { cmd: "save_all", data: $scope.tagOps }
    return

  $scope.savegood = ->
    act.toActor { cmd: "save_good", data: $scope.tagOps }
    return

  $scope.do_parse = (data) ->
    $.ajax({
    url: "../api/juman/unique_words",
    type: "POST",
    contentType: "text/plain; charset=UTF8",
    dataType: "text",
    processData: false,
    data: data
    }).done (text) ->
      $scope.$apply ->
        $scope.rawText = ""
        cur = $scope.words
        $scope.words = cur + '\n#parsed\n' + text
