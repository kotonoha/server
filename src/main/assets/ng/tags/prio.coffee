koto = angular.module('kotonoha')

ctr = ($scope, tagSvc) ->
  tagSvc.callback = (msg) ->
    switch msg.cmd
      when "tags"
        $scope.$apply -> $scope.tags = msg.data
    return

  $scope.priorities = [
    { value: -200, name: "Lowest" }
    { value: -100, name: "Low" }
    { value: 0, name: "Normal" }
    { value: 100, name: "High" }
    { value: 200, name: "Highest" }
  ]

  $scope.save = ->
    tagSvc.toActor
      cmd: "save"
      data: $scope.tags

  $scope.fixLimit = (tag) ->
    if (tag.priority == 0 && tag.limit == 5) then tag.limit = null
    if (tag.priority > 0 && (!tag.limit? || tag.limit == 0)) then tag.limit = 5

  return

koto.controller('Tags', ["$scope", "tagSvc", ctr])
