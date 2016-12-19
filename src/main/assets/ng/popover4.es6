/**
 * @author eiennohito
 * @since 2016/08/03
 */

let popover4 = angular.module('ui.popover4', []);

popover4.directive('popover4', [() => {
    return {
      restrict: 'A',
      template: "<div ng-transclude></div>",
      controller: () => {
        return {
          pobj: null,
          $onDestroy: function() { if (this.pobj != null) this.pobj.popover('dispose'); },
          toggle: function () { if (this.pobj !== null) this.pobj.popover('toggle') }
        };
      },
      transclude: true,
      link: (scope, elem, attr, ctrl, transclude) => {
        /*if (!attr.enabled) {
          elem.remove();
          return
        }*/

        if (attr['popover4'] !== undefined) {
          let name = attr['popover4'];
          scope[name] = ctrl;
        }

        let parent = elem.parent();
        let po = parent.popover({
          container: "body",
          content: elem,
          html: true,
          trigger: "manual",
          placement: "bottom"
        });

        if (!attr['popover4Manual']) {
          po.on('click', () => {
            po.popover('toggle');
          });
        }

        elem.detach();

        ctrl.pobj = po;
      }
    }
}]);






