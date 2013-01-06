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
    active = false
    sel_active = true
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
        $scope.input = null
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

    move = (hm) ->
      items = $('li.tag-ac-entry', $element)
      sel = $('li.tag-ac-sel', $element)
      pos = items.index(sel)
      len = items.size()
      pos = 0 if (pos == -1)
      npos = (len + pos + hm) % len
      items.removeClass('tag-ac-sel-def tag-ac-sel')
      elm = $(items.get(npos))
      elm.addClass('tag-ac-sel')
      sel_active = true
      #scrolling
      p = elm.position()
      cont = $ 'div.taglist-ac-container', $element
      top = cont.scrollTop()
      hgt = cont.height()
      itop = top + p.top
      ibot = top + p.top + sel.height()
      step = cont.height() - sel.height() / 2
      ns = top
      if (npos > pos) #going down
        ns += step while ibot > (ns + hgt)
      else
        ns -= step while itop < ns

      cont.scrollTop(ns) if ns != top


    fixup_selection = ->
      work = ->
        if $('li.tag-ac-sel, li.tag-ac-sel-def', $element).size() == 0
          move(0)
          $('li.tag-ac-entry', $element).removeClass('tag-ac-sel-def').first().addClass('tag-ac-sel-def')
          sel_active = false
      setTimeout(work, 100)

    rename_1st_part = (curtext) ->
      old = curtext
      mode = 'to'
      $scope.$apply ->
        $scope.padding = "#{old} ↦ "
        $scope.input = null
      inpel.val("")

    select_candidate = ->
      $('li.tag-ac-sel, li.tag-ac-sel-def', $element).click()

    pending_remove = false
    exp_removing = null

    remove_last = ->
      if (pending_remove && exp_removing?)
        pending_remove = false
        $('a', exp_removing).click()
        exp_removing = null

    prepare_to_remove = ->
      d = $scope.data
      if (d.length == 0)
        return false
      last = d[d.length - 1]
      if typeof last == 'string'
        return false
      exp_removing = $('.tag-item', $element).last()
      exp_removing.addClass('tag-prep-rm')
      pending_remove = true

    cleanup_remove = ->
      if (pending_remove)
        exp_removing.removeClass('tag-prep-rm')
        exp_removing = null
        pending_remove = false

    inp_keypress = (evt) ->
      curtext = inpel.val()
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
          else if sel_active && (evt.which == 13 || evt.which == 44 || evt.which == 59)
            select_candidate()
          else
            mode = 'add'
            inputMode('icon-plus')
            fixup_selection()
            return true
          return false
      if mode == 'rename' && evt.which == 47 #second slash
          rename_1st_part(curtext)
          return false
      switch evt.which
        #enter, comma, semicolon
        when 13, 44, 59
          if sel_active
            select_candidate()
          else if (!resolveInput(curtext))
            showError()
          return false
        else
          #do nothing
      fixup_selection()
      return true

    inp_keydown = (evt) ->
      curtext = inpel.val()
      if (curtext == "" && evt.which == 8)
        if mode == 'to'
          $scope.$apply ->
            pad = $scope.padding
            $scope.padding = ""
            mode = 'rename'
            inpel.val(pad.substring(0, pad.length - 3))
          return false
        if (mode == null)
          if pending_remove then remove_last() else prepare_to_remove()
        mode = null
        inputMode('')
        return false
      else if evt.which == 38 #up arrow
        if active then move(-1); return false
        else return true
      else if evt.which == 40 #down arrow
        if active then move(1); return false
        else return true
      else if evt.which == 9
        if active
          select_candidate()
          cleanup_remove()
          return false
      cleanup_remove()
      return true

    select_tag = (tag, elem) ->
      if mode == null
        mode = 'add'
      if mode == 'rename'
        rename_1st_part(tag.name)
      else
        resolveInput(tag.name)
      $('li.tag-ac-sel', $element).removeClass('tag-ac-sel')
      fixup_selection()
      return false


    remove_for: (x) -> $scope.$apply $scope.data.push({remove: x})
    delete_item: (x) ->
      idx = $scope.data.indexOf(x)
      if idx != -1
        $scope.$apply $scope.data.splice(idx, 1)
    input_keypress: inp_keypress
    input_keydown: inp_keydown
    select_tag_from_ac: (o1, o2) ->
      f = -> select_tag(o1, o2)
      setTimeout(f, 0)
    focused: ->
      active = true
      fixup_selection()
    blurred: ->
      active = false
      $('li.tag-ac-entry', $element).removeClass('tag-ac-sel tag-ac-sel-def')



  template: """
            <div class='taglist'>
              <ul class='tag-container'>
                <li class='tagop tag-item' ng-repeat='obj in data'></li>
                <li class='taginput'><i/><span class='tagpadding'>{{padding}}</span><input ng-model='input.name'></li>
              </ul>
              <div class='taglist-ac-container'>
                <ul>
                  <li class='tag-group' ng-repeat='gr in tagnfo | groupNotEmpty:input'>
                    <span class='tag-group-label'>{{gr.group}}</span>
                    <ul>
                      <li
                        class='tag-ac-entry'
                        ng-click='tag_select(tag, this)'
                        ng-repeat='tag in gr.tags | filter:input'>{{tag.name}}: {{tag.descr}}</li>
                    </ul>
                  </li>
                </ul>
              </div>
            </div>
            """
  scope:
    data: "="
    tagnfo: "="
  link: (scope, elem, attrs, contr) ->

    input = $('input', elem)
    cont = $ '.tag-container', elem

    acCont = $ '.taglist-ac-container', elem
    acCont.css({width: (cont.width() + 10) + "px"})

    should_hide = true

    input.focus ->
      contr.focused()
      cont.addClass('tag-container-active')
      should_hide = false
      acCont.show()
    input.blur ->
      contr.blurred()
      should_hide = true
      hideFn = ->
        if should_hide
          acCont.hide()
      setTimeout(hideFn, 250)
      cont.removeClass('tag-container-active')

    input.keypress contr.input_keypress
    input.keydown contr.input_keydown

    acCont.scroll -> input.focus()

    scope.tag_select = (tag, elem) ->
      contr.select_tag_from_ac(tag, elem)
      input.focus()

    cont.click -> input.focus()

module.filter 'groupNotEmpty', ($filter) ->
  (grps, obj) ->
    arr = []
    flt = $filter('filter')
    for gr in grps
      ot = flt(gr.tags, obj)
      arr.push(gr) if (ot.length > 0)
    arr

