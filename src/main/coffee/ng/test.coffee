tens = (start) ->
  ({name: i, tens: i * 10} for i in [start..start+10])

module = angular.module('kotonoha', ['taglist'])

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

module.directive 'dataGrid', ($injector) ->
  restrict: 'A'
  transclude: false
  scope:
    expr: '@'
    exp2: '@dataGrid'
  controller: ($scope, $elem, $attrs) ->
    {}
  compile: (el, attrs, trans) ->
    return



window.TC2 = ($scope) ->
  $scope.objs = [
    { name: 'x', id: 1, gr: 'last'}
    { name: 'y', id: 2, gr: 'last'}
    { name: 'z', id: 3, gr: 'last'}
    { name: 'a', id: 4, gr: 'fst'}
    { name: 'b', id: 5, gr: 'fst'}
  ]
  $scope.whatever = ["tag1", {add: "tag2"}, {remove: "tag3"}, {rename: "tag4", to: "tag5"}]


module.directive 'chosen', ->
  linker = (scope, elem, attr) ->
    elem.chosen()
    chosen = elem.data('chosen')
    scope.$watch attr['chosen'], ->
      elem.trigger('liszt:updated')
      return
    return

  {
    restrict: 'A',
    link: linker
    priority: 20
  }
