angular.module('kotonoha', ['taglist'])

window.WordCon = ($scope, $http) ->
  re = /w=([0-9a-f]+)/i
  [_, id] = window.location.search.match(re)
  $scope.status = false
  $http.get("../api/model/words/#{id}").success (w) -> $scope.word = w

  $scope.save = (command) ->
    $http.post("../api/model/words/#{id}",
      command: command
      content: $scope.word
    ).success ->
      $scope.status = true
      w = $scope.word
      if (command == 'update-approve')
        w.status = 'Approved'
      $scope.word = w

  $scope.delete = ->
    $http.delete("../api/model/words/#{id}").success ->
      $scope.word.status = "Deleting"

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

  $scope.select_all = -> selection(-> true)
  $scope.select_none = -> selection(-> false)
  $scope.select_invert = -> selection((e) -> !e.selected)

  return
