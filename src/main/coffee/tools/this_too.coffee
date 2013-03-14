mod = angular.module('ui.this_too', [])

thisTemplate = """<button class='btn btn-mini'><i class='icon-plus'></i> add me</button>"""

publish_candidate = (item) ->
  out = [item]
  $.ajax
    url: "/api/events/add_words"
    type: "post"
    dataType: "json"
    contentType: "application/json; charset=UTF-8"
    processData: false
    data: JSON.stringify(out)

#angular-free version
window.resolve_thistoo = (id, obj) ->
  elem = $("#"+id)
  content = $(thisTemplate)
  content.appendTo(elem)
  content.click ->
    publish_candidate(obj)
    elem.hide 350, -> elem.remove()
    return


mod.factory 'thisTooSvc', ($rootScope) ->
  myscope = $rootScope.$new(true)
  window.resolve_thistoo = (id, obj) ->
    myscope.$apply -> myscope[id] = obj
  {
    scope: myscope
  }


mod.directive 'thisToo', (thisTooSvc) ->
  restrict: 'A'
  template: """<button ng-show='full' class='btn btn-mini'><i class='icon-plus'></i> add me</button>"""
  scope: true
  replace: false
  link: (scope, elem, attr, cont) ->
    id = attr.id
    thisTooSvc.scope.$watch id, (nval) ->
      scope.full = true
      scope.data = nval
      return
    $('.btn', elem).click ->
      if (scope.full)
        publish_candidate(scope.data).success
      elem.hide 350, -> elem.remove()
      return
