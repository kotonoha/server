mod = angular.module('kotonoha', ['taglist', 'ui.autosize'])

ctor = ($scope, $http) ->
  re = /w=([0-9a-f]+)/i
  [_, id] = window.location.search.match(re)
  $scope.status = false
  load = ->
    q = $http.get("../api/model/words/#{id}")
    q.then (w) ->
      $scope.word = w.data
    q

  load()

  $scope.save = (command) ->
    $http.post("../api/model/words/#{id}",
               command: command
               content: $scope.word
              ).then ->
                load().then ->
                  $scope.status = true

  $scope.delete = ->
    $http.delete("../api/model/words/#{id}").then ->
      load()

  $scope.loadAutoExamples = () ->
    $http.post("../api/model/words/#{id}/updateAutoExamples").then( -> load())

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

mod.controller("WordCon", ["$scope", "$http", ctor])
