// Theme JavaScript
(function (UIkit) {

    var util = UIkit.util,
        $ = util.$,
        attr = util.attr,
        css = util.css,
        addClass = util.addClass;

    UIkit.component('header', {

        name: 'header',

        connected: function () {
            this.initialize();
        },

        ready: function () {
            if (!this.section) {
                this.initialize();
            }
        },

        update: [

            {

                read: function () {
                    this.prevHeight = this.height;
                    this.height = this.$el.offsetHeight;
                    var sticky = this.modifier && UIkit.getComponent(this.sticky, 'sticky');
                    if (sticky) {
                        sticky.$props.top = this.section.offsetHeight <= window.innerHeight
                            ? this.selector
                            : util.offset(this.section).top + 300;
                    }
                },

                write: function () {
                    if (this.placeholder && this.prevHeight !== this.height) {
                        css(this.placeholder, {height: this.height});
                    }
                },

                events: ['load', 'resize']

            }

        ],

        methods: {

            initialize: function () {

                this.selector = '.tm-header ~ [class*="uk-section"], .tm-header ~ * > [class*="uk-section"]';
                this.section = $(this.selector);
                this.sticky = $('[uk-sticky]', this.$el);
                this.modifier = attr(this.section, 'tm-header-transparent');

                if (!this.modifier || !this.section) {
                    return;
                }

                addClass(this.$el, 'tm-header-transparent');

                this.placeholder = util.hasAttr(this.section, 'tm-header-transparent-placeholder')
                    && util.before($('[uk-grid]', this.section), '<div class="tm-header-placeholder uk-margin-remove-adjacent" style="height: ' + this.$el.offsetHeight + 'px"></div>');

                var container = $('.uk-navbar-container', this.$el),
                    navbar = $('[uk-navbar]', this.$el),
                    cls = 'uk-navbar-transparent uk-' + this.modifier;

                addClass($('.tm-headerbar-top, .tm-headerbar-bottom'), 'uk-' + this.modifier);

                if (attr(navbar, 'dropbar-mode') === 'push') {
                    attr(navbar, 'dropbar-mode', 'slide');
                }

                if (!this.sticky) {
                    addClass(container, cls);
                } else {
                    attr(this.sticky, {
                        animation: 'uk-animation-slide-top',
                        top: this.selector,
                        'cls-inactive': cls
                    });
                }
            }

        }

    });

    if (UIkit.util.isRtl) {

        var mixin = {

            init: function () {
                this.$props.pos = util.swap(this.$props.pos, 'left', 'right');
            }

        };

        UIkit.mixin(mixin, 'drop');
        UIkit.mixin(mixin, 'tooltip');

    }

})(UIkit);
