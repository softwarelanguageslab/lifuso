String json = "...
ObjectMapper<String> m = new ObjectMapper();
Set<Product> products = m.readValue(json, new TypeReference<Set<Product>>() {})
                        .anotherMethod()
                        .toString("skdjfbdskfjbsk")
