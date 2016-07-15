mod = angular.module('kotonoha', [])

time = -> new Date().getTime()

between_reqs = 1000 #1 sec

mod.controller 'WikiEditPage', ["$scope", "WikiEdit", (ctx, edit) ->

  last_answer = time() # can do it after first 5 secs
  last_request = time() - between_reqs
  req_in_progress = false
  send_pending = false

  pend_request = ->
    if (!send_pending)
      fn = -> process_send(true)
      setTimeout(fn, 250) # wait 2 secs
      console.log("pended request in 2 secs")
    send_pending = true

  perform_request = ->
    last_request = time()
    edit.toActor
      cmd: "update"
      data:
        src: ctx.src
        comment: ctx.comment

  process_send = (timeout) ->
    console.log("called process_send", timeout)
    if (timeout) then send_pending = false
    if (req_in_progress)
      pend_request()
    else #no request in progress
      tm = time()
      if (tm - last_answer > between_reqs)
        perform_request()
        req_in_progress = true
      else
        pend_request()

  ctx.src = ""
  ctx.ready = false

  ctx.$watch "src", (mod, old) ->
    if (!send_pending)
      fn = -> process_send(false)
      setTimeout(fn, 0)

  display = (data) ->
    ctx.$apply ->
      ctx.src = data?.src || ""
      ctx.ready = true

  preview = (data) ->
    last_answer = time()
    req_in_progress = false
    $('#preview').html(data)

  edit.callback = (c) ->
    switch c.cmd
      when "preview" then preview(c.src)
      when "display" then display(c.data)
      else console.log(c)


  ctx.save = (e) ->
    cmd =
      cmd: "save"
      data: { src: ctx.src, comment: ctx.comment }
    edit.toActor(cmd)
    return

  ctx.cancel = (e) ->
    return
]
