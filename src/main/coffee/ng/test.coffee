tens = (start) ->
  ({name: i, tens: i * 10} for i in [start..start+10])

module = angular.module('kotonoha', ['ui.autosize', 'ui.popover2'])

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
  $scope.tagNfo = [
    { group: 'User tags', tags: [
      { name: 'tag1', descr: 'tag number 1, a great tag'}
      { name: 'n', descr: 'Regular noun (Futsuumeishi — 普通名詞)'}
    ]},
    { group: 'System tags', tags: [
      { name: 'exp', descr: 'Expression'}
      { name: '4j', descr: 'Yojijukugo'}
    ]},
    { group: "Test tags", tags: [
      { name: 't1', descr: 't2'}
      { name: 't1', descr: 't2'}
      { name: 't1', descr: 't2'}
      { name: 't1', descr: 't2'}
      { name: 't1', descr: 't2'}
      { name: 't1', descr: 't2'}
      { name: 't1', descr: 't2'}
      { name: 't1', descr: 't2'}
      { name: 't1', descr: 't2'}
      { name: 't1', descr: 't2'}
    ]}
  ]
  $scope.text = "This is a test. Don't worry, be happy!"


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
