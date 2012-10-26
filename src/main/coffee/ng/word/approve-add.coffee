angular.module('kotonoha').controller('AddWord', ($q, AddSvc, $scope) ->
  svc = AddSvc
  $scope.waiting = true;
  $scope.loaded = false;
  $scope.btns_visible = false;
  nextItems = []
  $scope.cur = 0
  $scope.total = 0

  updateIndices = (obj) ->
    $scope.total = obj.total;

  display = () ->
    $scope.waiting = false
    $scope.btns_visible = true
    obj = nextItems.shift()
    $scope.word = obj.word
    $scope.dics = obj.dics
    updateIndices(obj)

  processNext = ->
    if (nextItems.length != 0)
      display() #just display next word
    else
      $scope.waiting = true
      $scope.btns_visible = false


  $scope.save = (status) ->
    cmd = {
      cmd: "save"
      status: status
      word: $scope.word
    }
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

  $scope.addFromDic = (de, ev) ->
    cmd =
      cmd: "add-from-dic"
      entry: de
    svc.toActor(cmd)
    $(ev.target).hide(200)
    return false;


  svc.callback = (obj) ->
    if (!$scope.loaded) then $scope.$apply -> $scope.loaded = true;
    if (obj.cmd?) then switch(obj.cmd)
      when "word"
        nextItems.push(obj)
        if ($scope.waiting) then $scope.$apply -> display()
      when "update-indices" then $scope.$apply -> updateIndices(obj)
      when "no-items"
        $scope.waiting = false;

)
