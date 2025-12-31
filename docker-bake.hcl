group "default" {
  targets = ["store", "payment"]
}

target "common" {
  context = "."
  dockerfile_inline = ""
}

target "store" {
  inherits = ["common"]
  dockerfile = "store/Dockerfile"
  tags = ["my-market/store:latest"]
}

target "payment" {
  inherits = ["common"]
  dockerfile = "payment/Dockerfile"
  tags = ["my-market/payment:latest"]
}