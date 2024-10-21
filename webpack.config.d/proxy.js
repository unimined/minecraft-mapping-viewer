if (config.devServer) {
    config.devServer.proxy = [
        {
            context: ["/kv/*", "/kvsse/*"],
            target: 'http://localhost:8080'
        },
        {
            context: ["/kvws/*"],
            target: 'http://localhost:9000',
            ws: true
        }
    ]
}