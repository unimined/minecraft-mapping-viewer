if (config.devServer) {
    config.devServer.proxy = [
        {
            context: ["/kv/*", "/kvsse/*"],
            target: 'http://localhost:9000'
        },
        {
            context: ["/kvws/*"],
            target: 'http://localhost:9000',
            ws: true
        }
    ]
}