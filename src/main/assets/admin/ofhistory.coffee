max = (data, val, init) ->
  data.forEach (o) ->
    x = o[val]
    if (x > init) then init = x
  init

calc_header = (data) ->
  x = {}
  for o in data
    x[o.ef] = 1
  arr = (ef for ef of x)
  arr.sort()
  arr

renderHeader = (hdrs) ->
  row = $("<tr></tr>")
  row.append $("<td><b>#{o}</b></td>") for o in hdrs
  row

renderRow = (data, n, mapper) ->
  row = $("<tr></tr>")
  row.append $("<td></td>") for i in [1..mapper.length]

  for o in data when o.rep == n
    idx = mapper.indexOf(o.ef.toString())
    val = o.value.toFixed(2)
    diff = o.diff.toFixed(2)
    col = null
    if (diff > 0)
      g = (255 * Math.min(1.0, diff)).toFixed(0)
      col = "rgb(#{255 - g}, 255, #{255 - g})"
    else
      r = (255 * Math.min(1.0, -diff)).toFixed(0)
      col = "rgb(255, #{255 - r}, #{255 - r})"
    marks = (x for x in o.marks)
    hasMark = o.marks.reduce((i, j) -> i + j) != 0
    nfo1 = $("<div>#{val} #{diff}</div>").css('background-color', col)
    nfo2 = $("<div>#{marks}</div>")
    if (hasMark)
      nfo2.addClass('has-value')
    $(row.children()[idx]).append(nfo1).append(nfo2)

  row

renderMatrix = (data) ->
  hdrdata = calc_header(data)
  tbl = $("<table></table>")
  renderHeader(hdrdata).appendTo(tbl)
  maxn = max(data, "rep", 1)
  for i in [1...maxn]
    renderRow(data, i, hdrdata).appendTo(tbl)

  x = $("#oftbl")
  x.empty().append(tbl)


lastItem = null

pushHistoryItem = (id) ->
  if (lastItem == null)
    lastItem = id
  else
    lit = lastItem
    lastItem = null
    $.ajax(
      url: "../api/admin/ofhistory/compare"
      dataType: "json"
      data:
        l: lit
        r: id
    ).done (res) ->
      renderMatrix(res)

createTbl = (data) ->
  for obj in data
    cont = $('<div></div>', {class: "link-cont"}).text(obj.email)
    for v in obj.data
      dt = new Date(v.time)
      tag = $("<a>#{dt.getMonth() + 1}-#{dt.getDate()}</a>", {href: "#"}).click v.id, (e) ->
        pushHistoryItem(e.data)
      tag.appendTo(cont)
    cont.appendTo("#enttbl")


makeTbl = ->
  $.ajax(
    url: "../api/admin/ofhistory"
    dataType: "json"
  ).done (data) ->
    $( -> createTbl(data) )

makeTbl()

