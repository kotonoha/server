resizer = (el) ->
  resize = ->
    el.rows = 1
    $(el).css('height', 'auto')
    $(el).css('height', "#{el.scrollHeight}px")
    false

  delayedResize = ->
    window.setTimeout resize, 0
    return

  $(el).on "change", resize
  $(el).on "cut paste drop keydown", delayedResize

  delayedResize()

  {
    resize: resize
    delayedResize: delayedResize
  }


$.fn.autoresize = ->
  this.each (i) -> resizer(this, i)


angular.module("kotonoha").directive "autoresize", ->
  restrict: "C"
  scope: false
  link: (scope, el, attrs, cont) ->
    res = resizer(el[0])
    if (attrs.ngModel?)
      name = attrs.ngModel
      scope.$watch(name, -> res.delayedResize())
