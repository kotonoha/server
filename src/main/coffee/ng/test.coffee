tens = (start) ->
  ({name: i, tens: i * 10} for i in [start..start+10])

window.Model = ($scope) ->
  $scope.start = 23
  $scope.regenerate = -> $scope.items = tens($scope.start)
  $scope.regenerate()

angular.module('kotonoha', []).directive 'dataGrid', ($injector) ->
  restrict: 'A'
  transclude: false
  scope:
    expr: '@'
    exp2: '@dataGrid'
  controller: ($scope, $elem, $attrs) ->
    {}
  compile: (el, attrs, trans) ->
    return
