module = angular.module('kotonoha', [])


height = 400
width = 600

tree = d3.layout.tree().size [height, width]

diagonal = d3.svg.diagonal().projection (d) -> [width - d.y, d.x]

i = 0

update_tree = (elem, object, oldval) ->
  if !object?
    rootnode = {
      surface: []
    }
  else rootnode = object.node

  svge = d3.select(elem[0])
    .attr('width', width + 20).attr('height', height + 20)
  de =
    if (svge.selectAll('g').size() == 0)
      svge.append('g').attr('width', width).attr('height', height)
      .attr('transform', 'translate(10, 10)')
    else svge.select('g')

  nodes = tree.nodes(rootnode).reverse()
  links = tree.links(nodes)

  nodes.forEach (n) -> n.y = n.depth * 150

  node = de.selectAll('g.node').data(nodes, (d) ->
    d.id = i
    i += 1
    d.id
  )

  enter = node.enter().append("g")
        .attr("class", "node")
        .attr('transform', (d) -> "translate(#{width - d.y}, #{d.x})")


  enter.append('text')
      .selectAll('tspan').data((d) -> d.surface)
      .enter().append('tspan')
        .text((d) -> d.surface)
        .attr('text-anchor', 'middle')


  node.exit().remove()

  elems = {}

  node.each (d) -> elems[d.id] = this

  link = de.selectAll('path.link').data(links)

  link.enter().insert("path", "g")
    .attr("class", "link")
    .attr("d", (i) ->
      src = i.source
      trg = i.target
      sel = elems[src.id]
      tel = elems[trg.id]
      sbox = sel.getBBox()
      tbox = tel.getBBox()
      sx = sbox.height / 2;
      tx = tbox.height / 2;
      diagonal(
        target: { x: trg.x, y: trg.y },
        source: { x: src.x, y: src.y }
      )
    )

  link.exit().remove()


module.directive 'depgraph', () ->
  linker = (scope, elem, attr) ->
    scope.$watch "target", (n, o) ->
      update_tree(elem, n, o)

  {
    restrict: 'A',
    link: linker,
    scope: { target: "=depgraph"}
  }

window.KnpController = ($scope, knpService, $location) ->

  $scope.submit = (word) ->
    msg =
      'cmd': 'analyze',
      'content': word
    knpService.toActor(msg)

  publishResults = (obj) ->
    $scope.$apply ->
      $scope.node = obj
    #$location.search('query', obj.surface)

  search = $location.search()
  if (search.query?)
    $scope.submit(search.query)

  knpService.callback = (obj) ->
    switch obj?.cmd
      when "results" then publishResults(obj.content)
      else console.log(["invalid message from actor", obj])