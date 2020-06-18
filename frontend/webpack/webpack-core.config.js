var path = require("path");
var CopyWebpackPlugin = require("copy-webpack-plugin");
var HtmlWebpackPlugin = require("html-webpack-plugin");
const ExtractCssChunks = require("extract-css-chunks-webpack-plugin");

const postcssOptions = {
  config: {
    path: path.resolve(__dirname, "./postcss.config.js"),
  },
};


module.exports = {
  mode: "development",
  resolve: {
    alias: {
      resources: path.resolve(__dirname, "../../../../src/main/resources"),
      js: path.resolve(__dirname, "../../../../src/main/js"),
      scalajs: path.resolve(__dirname, "./scalajs-entry.js"),
    },
    modules: [path.resolve(__dirname, "node_modules")],
  },
  module: {
    rules: [
      // {
      //   test: /\.css$/,
      //   use: ["style-loader", "css-loader"],
      // },
      {
        test: /\.css$/,
        use: [
          {
            loader: ExtractCssChunks.loader,
            options: {
              filename: "[name].[contenthash:8].[ext]",
            },
          },
          {
            loader: "css-loader",
          },
          {
            loader: "postcss-loader",
            options: postcssOptions,
          },
        ],
      },
      {
        test: /\.scss$/,
        use: [
          {
            loader: ExtractCssChunks.loader,
            options: {
              filename: "[name].[contenthash:8].[ext]",
            },
          },
          {
            loader: "css-loader",
          },
          {
            loader: "postcss-loader",
            options: postcssOptions,
          },
          {
            loader: "sass-loader",
          },
        ],
      },
      // "file" loader for svg
      {
        test: /\.(svg|png|ico)$/,
        use: [
          {
            loader: "file-loader",
            query: {
              name: "static/media/[name].[hash:8].[ext]",
            },
          },
        ],
      },
      {
        test: /\.md$/,
        use: [
          {
            loader: "raw-loader"
          }
        ]
      }
    ],
  },
  plugins: [
    new ExtractCssChunks({
      filename: "[name].[hash].css",
      chunkFilename: "[id].css",
    }),
    new CopyWebpackPlugin([
      {
        from: path.resolve(__dirname, "../../../../public"),
        to: path.resolve(__dirname, "../../../../../backend/public"),
      },
    ]),
    new HtmlWebpackPlugin({
      template: path.resolve(__dirname, "../../../../public/index.html"),
      filename: path.resolve(
        __dirname,
        "../../../../../backend/public/index.html"
      ),
    }),
  ],
  output: {
    devtoolModuleFilenameTemplate: (f) => {
      if (
        f.resourcePath.startsWith("http://") ||
        f.resourcePath.startsWith("https://") ||
        f.resourcePath.startsWith("file://")
      ) {
        return f.resourcePath;
      } else {
        return "webpack://" + f.namespace + "/" + f.resourcePath;
      }
    },
  },
};
