mod = angular.module('collapsable', [])

mod.directive 'collapsable', () ->
  {
  restrict: 'C'
  scope:
    title: "@"
  transclude: true
  replace: true
  template: '<div><div class="title">{{title}}</div><div class="body" ng-transclude></div></div>'
  link: (scope, elem, attrs) ->
    title = $(elem.children()[0])
    opened = true
    toggle = ->
      opened = !opened
      elem.removeClass(if opened then 'closed' else 'opened')
      elem.addClass(if opened then 'opened' else 'closed')

    title.bind 'click', toggle
    toggle()
  }

mod.directive 'dropdown', ($document) ->
  openElement = null
  close = null
  restrict: 'CA'
  link: (scope, elem, attrs) ->
    elem.bind "click", (e) ->
      e.preventDefault()
      e.stopPropagation()

      iWasOpen = false

      if (openElement)
        iWasOpen = openElement == elem
        close()

      if !iWasOpen
        elem.parent().addClass('open')
        openElement = elem

        close = (e) ->
          if (e)
            e.preventDefault()
            e.stopPropagation()
          $document.unbind 'click', close
          elem.parent().removeClass('open')
          close = null
          openElement = null

        $document.bind 'click', close
