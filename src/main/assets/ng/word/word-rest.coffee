angular.module('kotonoha', ['taglist', 'ui.autosize'])

window.WordCon = ($scope, $http) ->
  re = /w=([0-9a-f]+)/i
  [_, id] = window.location.search.match(re)
  $scope.status = false
  load = ->
    q = $http.get("../api/model/words/#{id}")
    q.success (w) ->
      $scope.word = w
    q

  load()

  $scope.save = (command) ->
    $http.post("../api/model/words/#{id}",
               command: command
               content: $scope.word
              ).success ->
                load().success ->
                  $scope.status = true

  $scope.delete = ->
    $http.delete("../api/model/words/#{id}").success ->
      load()

  $scope.addExample = ->
    $scope.word.examples.push
      selected: true
      example: ""
      translation: ""

  selection = (fnc) ->
    exs = $scope.word.examples
    for ex in exs
      ex.selected = fnc(ex)
    return

  $scope.select_all = ->
    selection(->
      true)
  $scope.select_none = ->
    selection(->
      false)
  $scope.select_invert = ->
    selection((e) ->
      !e.selected)

  return
