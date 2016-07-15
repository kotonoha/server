mod = angular.module( 'ui.popover2', [] )

displaying = null

mod.directive 'popover2', ( $compile, $timeout, $parse, $window ) ->
  template =  '''
              <div class="popover {{placement}}" ng-class="{ in: isOpen }">
    <div class="arrow"></div>

    <div class="popover-inner">
        <h3 class="popover-title" ng-bind="popoverTitle" ng-show="popoverTitle"></h3>
        <div ng-transclude></div>
    </div>
  </div>
  '''

  scope: { popoverTitle: '@', animation: '&', enabled: '@' }
  transclude: true
  replace: true
  template: template
  link: ( scope, element, attr ) ->
      popover = element
      transitionTimeout = null
      parent = element.parent()
      #element.detach()

      attr.$observe 'placement', (val) -> scope.placement = val || 'top'

      # By default, the popover is not open.
      scope.isOpen = false;

      # Calculate the current position and size of the directive element.
      getPosition = ->
        boundingClientRect = parent[0].getBoundingClientRect()
        width: parent.prop( 'offsetWidth' ),
        height: parent.prop( 'offsetHeight' ),
        top: boundingClientRect.top + $window.pageYOffset,
        left: boundingClientRect.left + $window.pageXOffset

      show = ->
        if (!scope.enabled) then return
        # If there is a pending remove transition, we must cancel it, lest the
        # toolip be mysteriously removed.
        if ( transitionTimeout )
          $timeout.cancel( transitionTimeout )

        # Set the initial positioning.
        popover.css({ top: 0, left: 0, display: 'block' })

        # Now we add it to the DOM because need some info about it. But it's not
        # visible yet anyway.
        #element.appendTo(parent)

        # Get the position of the directive element.
        position = getPosition()

        # Get the height and width of the popover so we can center it.
        ttWidth = popover.prop( 'offsetWidth' )
        ttHeight = popover.prop( 'offsetHeight' )

        # Calculate the popover's top and left coordinates to center it with
        # this directive.
        ttPosition = null
        switch ( scope.placement )
          when 'right'
            ttPosition = {
              top: (position.top + position.height / 2 - ttHeight / 2) + 'px',
              left: (position.left + position.width) + 'px'
            }
            break
          when 'bottom'
            ttPosition = {
              top: (position.top + position.height) + 'px',
              left: (position.left + position.width / 2 - ttWidth / 2) + 'px'
            }
            break
          when 'left'
            ttPosition = {
              top: (position.top + position.height / 2 - ttHeight / 2) + 'px',
              left: (position.left - ttWidth) + 'px'
            }
            break
          else
            ttPosition = {
              top: (position.top - ttHeight) + 'px',
              left: (position.left + position.width / 2 - ttWidth / 2) + 'px'
            }


        # Now set the calculated positioning.
        popover.css( ttPosition )

        # And show the popover.
        scope.isOpen = true
        if (displaying != null) then displaying()
        displaying = hide
        return

      # Hide the popover popup element.
      hide = ->
        # First things first: we don't show it anymore.
        #popover.removeClass( 'in' );
        scope.isOpen = false

        # And now we remove it from the DOM. However, if we have animation, we
        # need to wait for it to expire beforehand.
        # FIXME: this is a placeholder for a port of the transitions library.
        if ( angular.isDefined( scope.animation ) && scope.animation() )
          fun = -> popover.css {display: 'none'}
          transitionTimeout = $timeout( fun, 500 )
        else
          popover.css {display: 'none'}
        displaying = null
        return

      # Register the event listeners.
      parent.bind 'click', ->
        if(scope.isOpen)
            scope.$apply( hide )
        else
            scope.$apply( show )
