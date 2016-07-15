mod = angular.module('ui.floating', [])

mod.directive 'floating', () ->
  restrict: 'AC'
  link: (scope, elem, attrs) ->
    pwidth = elem.parent().width()
