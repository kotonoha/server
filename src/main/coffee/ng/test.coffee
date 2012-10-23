tens = (start) ->
  ({name: i, tens: i * 10} for i in [start..start+10])

window.Model = ($scope, testSvc) ->
  $scope.start = 23
  $scope.regenerate = -> $scope.items = tens($scope.start)
  $scope.regenerate()
  $scope.text = "This is text"
  $scope.rtext = -> $scope.text = "This is text0"

  $scope.toArms = ->
    testSvc.toActor([$scope.text, $scope.text])

  this.myfunc = (x) ->
    $scope.x = x;

  testSvc.callback = (o) ->
    console.log(o)
    if (o.hey?)
      $scope.$apply ->
        $scope.xlen = o.length

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
