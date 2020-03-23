if (process.env.NODE_ENV === "production") {
    const opt = require("./frontend-opt.js");
    opt.main();
    module.exports = opt;
} else {
    var exports = window;
    exports.require = require("./frontend-fastopt-entrypoint.js").require;
    window.global = window;

    const fastOpt = require("./frontend-fastopt.js");
    fastOpt.main()
    module.exports = fastOpt;

    if (module.hot) {
        module.hot.accept();
    }
}
