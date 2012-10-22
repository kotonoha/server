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
      $scope.word = w

  $scope.delete = ->
    $http.delete("../api/model/words/#{id}").success ->
      $scope.word.status = "Deleting"

  $scope.addExample = ->
    $scope.word.examples.push
      selected: true
      example: ""
      translation: ""

  return
