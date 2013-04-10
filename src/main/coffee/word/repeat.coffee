items = []
item = null
now = -> new Date().getTime()
time = now()
last = []
MODE_QUESTION = 0
MODE_ANSWER = 1
MODE_NEXT = 2
mode = -1
state = 'up'

in_last = (cid) ->
  for id in last
    if (id == cid)
      return true
  false

update_link = null

window.publish_new = (list) ->
  parsed = $.parseJSON(list)
  items = (it for it in parsed when not in_last(it.cid))
  display_next() if item == null

display_no_items = ->
  $('#button-pane').hide()

display_item = (item) ->
  $('#button-pane').show()
  show_question(item.mode)
  $("#word-writing").text(item.writing);
  $("#word-reading").text(item.reading);
  $("#word-meaning").html(item.meaning.replace("\n", "<br/>"));
  $("#example-pane").html(item.examples);
  $("#word-additional").html(item.additional);

display_next = ->
  if items?.length != 0
    next = items.shift()
    item = next
    if (update_link != null)
      update_link(item)
    display_item(next)
    time = now()
    if (last.length >= 12)
      last.shift()
    last.push(item.cid)
  else
    if update_link != null then update_link(null)
    display_no_items()

mark_displayed = (mark) ->
  mode = MODE_NEXT
  $('#mark-pane').hide()
  $('#next-word-pane').show()

  timeMill = now() - time
  timeDouble = timeMill / 1000.0
  obj =
    card: item.cid
    mode: item.mode
    time: timeDouble
    mark: mark
    remaining: items.length

  send_to_actor(obj)

show_answer = ->
  mode = MODE_ANSWER

  $("#show-answer-pane").hide();
  $("#word-additional").show();
  $("#word-writing").show();
  $("#word-reading").show();
  $("#word-meaning").show();
  $("#example-pane").show();
  $("#mark-pane").show();

hide_all = ->
  $("#word-writing").hide();
  $("#word-reading").hide();
  $("#word-meaning").hide();
  $("#example-pane").hide();
  $("#word-additional").hide();
  $("#mark-pane").hide();
  $("#next-word-pane").hide();
  $("#show-answer-pane").hide();
  $("div.word-display button").blur()

show_question = (qm) ->
  mode = MODE_QUESTION;
  hide_all();
  $("#show-answer-pane").show();
  if (qm == 1)
    $("#word-writing").show();
  else
    $("#word-reading").show();

eventmap =
  49: 1, 122: 1 # 1 and z
  50: 2, 120: 2 # 2 and x
  51: 3, 99: 3  # 3 and c
  52: 4, 118: 4 # 4 and v
  53: 5, 98: 5  # 5 and b


$(document).ready ->
  $(document).keyup (event) ->
    state = 'up'
    return

  $(document).keydown (event) ->
    if (event.which == 32)
      event.stopPropagation()
      event.preventDefault()
    if (state != 'up')
      return
    state = 'down'
    if (event.ctrlKey || event.altKey || event.shiftKey || event.metaKey)
      return
    switch mode
      when MODE_QUESTION
        if (event.which == 32)
          show_answer()
          return false
      when MODE_ANSWER
        mark = eventmap[event.which]
        mark_displayed(mark) if mark
      when MODE_NEXT
        if (event.which == 32)
          display_next()
          return false
    return

  $("#mark1").click -> mark_displayed(1)
  $("#mark2").click -> mark_displayed(2)
  $("#mark3").click -> mark_displayed(3)
  $("#mark4").click -> mark_displayed(4)
  $("#mark5").click -> mark_displayed(5)

  $("#show-next").click( -> display_next(); false )
  $("#show-answer").click( -> show_answer(); false )

window.RepeatController = ($scope) ->
  $scope.wid = null
  update_link = (item) ->
    $scope.$apply ->
      if (item == null)
        $scope.wid = null
      else $scope.wid = item.wid
