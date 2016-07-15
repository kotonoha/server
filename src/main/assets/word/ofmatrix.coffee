$(document).ready ->
  uri = "../api/ofmatrix"
  $.ajax {
    url: uri,
    dataType: "json",
    success: build_matrix
  }
  return


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
  row.append $("<td>#{o}</td>") for o in hdrs
  row

renderRow = (data, n, mapper) ->
  row = $("<tr></tr>")
  row.append $("<td></td>") for i in [1..mapper.length]

  for o in data when o.n == n
    idx = mapper.indexOf(o.ef.toString())
    $(row.children()[idx]).text(o.val.toFixed(2))

  row

build_ofmatrix = (data) ->
  obj = calc_header(data)
  maxn = max(data, "n", 0)

  tbl = $("#data-table")
  tbl.append(renderHeader(obj))
  for i in [1..maxn]
    tbl.append(renderRow(data, i, obj))

window.build_matrix = (data) ->
  build_ofmatrix(data)

  toDraw = ({x: o.ef - 1.2, y: o.val, z: o.n} for o in data)
  toDraw.sort(sortNumByZ)
  g = new canvasGraph('graph')
  g.xMax = max(toDraw, "x", 0) * 1.1
  g.yMax = max(toDraw, "y", 0) * 1.1
  g.zMax = max(toDraw, "z", 0) * 1.1
  #g.drawInfo()
  g.drawGraph(toDraw)

