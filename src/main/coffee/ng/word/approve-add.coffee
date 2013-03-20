mod = angular.module('kotonoha', ['taglist', 'ui.autosize'])

mod.controller 'AddWord', ($q, AddSvc, $scope) ->
  svc = AddSvc
  $scope.waiting = true;
  $scope.loaded = false;
  $scope.btns_visible = false;
  nextItems = []
  $scope.cur = 0
  $scope.total = 0

  $scope.tagNfo = []

  updateIndices = (obj) ->
    $scope.total = obj.total;

  display = () ->
    $scope.waiting = false
    $scope.btns_visible = true
    obj = nextItems.shift()
    $scope.word = obj.word
    $scope.dics = obj.dics
    $scope.tags = obj.tags
    updateIndices(obj)

  processNext = ->
    $scope.recommended = []
    if (nextItems.length != 0)
      display() #just display next word
    else
      $scope.waiting = true
      $scope.btns_visible = false


  $scope.save = (status) ->
    w = $scope.word
    $scope.word = null
    w.tags = $scope.tags
    cmd =
      cmd: "save"
      status: status
      word: w
    svc.toActor(cmd)
    $scope.cur += 1
    processNext()

  $scope.skip = ->
    cmd = { cmd: "skip", wid: $scope.word._id }
    svc.toActor(cmd)
    $scope.cur += 1
    processNext()

  $scope.addExample = ->
    $scope.word.examples.push({ example: "", translation: "", selected: true })

  $scope.copyDicEntry = (de) ->
    $scope.word.writing = de.writing
    $scope.word.reading = de.reading
    $scope.word.meaning = de.meaning
    cmd = { cmd: "retag", data: de }
    svc.toActor(cmd)

  $scope.addFromDic = (de, ev) ->
    cmd =
      cmd: "add-from-dic"
      entry: de
    svc.toActor(cmd)
    $(ev.target).hide(200)
    return false;

  selection = (fnc) ->
    exs = $scope.word.examples
    for ex in exs
      ex.selected = fnc(ex)
    return

  $scope.select_all = ->
    selection -> true
  $scope.select_none = ->
    selection -> false
  $scope.select_invert = ->
    selection((e) -> !e.selected)

  retag = (tags) ->
    old = $scope.tags
    max = 0
    for item, idx in old
      if typeof (item) == 'string'
        max = idx + 1
    tags.splice(0, 0, 0, max)
    Array.prototype.splice.apply(old, tags)

  handleRecommend = (req, resp) ->
    $scope.$apply -> $scope.recommended = resp

  $scope.process_recomendation = (item, ev) ->
    $(ev.target).parents('.rec-item').hide -> $scope.apply -> item.processed = true
    x = {}
    x.writing = item.writings.join(",")
    x.reading = item.readings.join(",")
    x.meaning = item.meanings.join("\n")
    cmd =
      cmd: "add-from-dic"
      entry: x

    svc.toActor(cmd)

  $scope.mark_rec_ignored = (item, ev) ->
    $(ev.target).parents('.rec-item').hide -> $scope.apply -> item.processed = true
    cmd =
      cmd: "ignore-rec"
      id: item.id
    svc.toActor(cmd)


  svc.callback = (obj) ->
    if (!$scope.loaded) then $scope.$apply ->
      $scope.loaded = true;
    if (obj.cmd?) then switch(obj.cmd)
      when "word"
        nextItems.push(obj)
        if ($scope.waiting) then $scope.$apply ->
          display()
      when "retag" then $scope.$apply ->
        retag(obj.tags)
      when "recommend" then handleRecommend(obj.request, obj.response)
      when "update-indices" then $scope.$apply ->
        updateIndices(obj)
      when "no-items"
        return

