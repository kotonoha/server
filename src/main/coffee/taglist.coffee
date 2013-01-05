$ = jQuery

module = angular.module('taglist', [])

module.directive 'tagop', ->
  restrict: 'C'
  require: "^taglist"
  link: (scope, elem, attrs, taglist) ->
    obj = scope.obj
    inner =
      if typeof(obj) == "string"
        link = $ "<a href='javascript:void(0);' onclick='return false;' class='btn-tiny'><i class='icon-remove'/></a>"
        link.click ->
          taglist.remove_for(obj)
        elem.addClass('tag-entry tag-noop')
        $("<span><i class='icon-tag'/> #{obj} </span>").append(link)
      else
        text = ""
        classes = ['tag-entry']
        icon = ""
        if obj.add?
          text = obj.add
          classes.push('tag-op-add')
          icon = 'icon-plus'
        else if obj.remove?
          text = obj.remove
          classes.push 'tag-op-remove'
          icon = 'icon-minus'
        else if obj.rename?
          text = "#{obj.rename} ↦ #{obj.to}"
          classes.push 'tag-op-rename'
          icon = 'icon-pencil'
        else
          text = "Error!!!!" + obj

        link = $ "<a href='javascript:void(0);' onclick='return false;' class='btn-tiny'><i class='icon-remove'></a>"
        span = $ "<span> #{text} </span>"
        elem.addClass(cl) for cl in classes
        icon = $ "<i class='#{icon}'/>"
        span.prepend(icon).append(link)
        link.click ->
          elem.fadeOut 200, ->
            taglist.delete_item(obj)
        span


    elem.append(inner)


module.directive 'taglist', ($compile) ->
  restrict: 'A'
  controller: ($scope, $element) ->
    mode = null
    old = ""
    inpel = $('.taginput input', $element)
    icon = $('.taginput i', $element)

    inputMode = (ic) ->
      icon.removeClass().addClass(ic)

    addAndClear = (obj) ->
      $scope.$apply ->
        $scope.data.push(obj)
        $scope.padding = ''
      mode = null
      inpel.val('')
      inputMode('')
      old = ""

    resolveInput = (text) ->
      if (!text? || text == '')
        return false
      if mode == 'add'
        addAndClear({add: text})
      if mode == 'remove'
        addAndClear({remove: text})
      if mode == 'to'
        addAndClear({rename: old, to: text})
      else return false
      return true

    showError = ->
      return 0


    inp_keypress = (evt) ->
      curtext = inpel.val()
      console.log(press: evt.which)
      if (curtext == "")
        if evt.which == 8
          return false
        if mode == null
          if evt.which == 45 # -
            mode = 'remove'
            inputMode('icon-minus')
          else if evt.which == 47 # /
            mode = 'rename'
            inputMode('icon-pencil')
          else
            mode = 'add'
            inputMode('icon-plus')
            return true
          return false
      if mode == 'rename' && evt.which == 47 #second slash
          old = curtext
          mode = 'to'
          $scope.$apply ->
            $scope.padding = "#{old} ↦ "
          inpel.val("")
          return false
      switch evt.which
        #enter, comma, semicolon
        when 13, 44, 59
          if (!resolveInput(curtext))
            showError()
          return false
        else
          return true
      return true

    inp_keydown = (evt) ->
      console.log(down: evt.which)
      curtext = inpel.val()
      if (curtext == "" && evt.which == 8)
        if mode == 'to'
          $scope.$apply ->
            pad = $scope.padding
            $scope.padding = ""
            mode = 'rename'
            inpel.val(pad.substring(0, pad.length - 3))
          return false
        mode = null
        inputMode('')
        return false
      return true


    remove_for: (x) -> $scope.$apply $scope.data.push({remove: x})
    delete_item: (x) ->
      idx = $scope.data.indexOf(x)
      if idx != -1
        $scope.$apply $scope.data.splice(idx, 1)
    input_keypress: inp_keypress
    input_keydown: inp_keydown



  template: """
            <ul class='tag-container'>
              <li class='tagop tag-item' ng-repeat='obj in data'></li>
              <li class='taginput'><i/><span class='tagpadding'>{{padding}}</span><input></li>
            </ul>
            """
  scope:
    data: "="
  link: (scope, elem, attrs, contr) ->

    input = $('input', elem)
    input.focus -> elem.addClass('tag-container-active')
    input.blur -> elem.removeClass('tag-container-active')

    input.keypress contr.input_keypress
    input.keydown contr.input_keydown

    elem.click -> input.focus()


