angular.module("kotonoha", []).directive("contenteditable", ->
    name: "editableSpan"
    restrict: 'A'
    require: "?ngModel"
    link: (scope, el, attrs, ngmod) ->
      if (!ngmod) then return

      ngmod.$render = ->
        val = ngmod.$viewValue
        if (val == null or val == '')
          el.text('Click here to edit!')
          el.addClass('empty')
        else
          el.text(val)

      read = ->
        ngmod.$setViewValue(el.text())

      el.bind 'blur keyup change', ->
        scope.$apply(read)

      el.bind 'focus', ->
        if (el.hasClass('empty'))
          el.text('')
          el.removeClass('empty')

      read()
      ngmod.$render()
)

angular.module("kotonoha").controller "Ctl", ($scope) ->
  $scope.halp = "test"
