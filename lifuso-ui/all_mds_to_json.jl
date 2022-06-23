mutable struct Feature
    title_meta::String
    library_meta::String
    name_meta::String
    name_data::String
    heading_data::String
    code_data::Vector{String}
    code_transformed::String
end

function to_html(text::AbstractString, tag::String, property::String="", property_values::String="")
    if property == ""
        return "<$tag>$text</$tag>"
    else
        return "<$tag $property=\\\"$property_values\\\">$text</$tag>"
    end
end

function features_to_json(features::Vector{Feature})
    json_text = "{\n\t\"body\": [\n"

    for feature in features
        json_text *= "\t\t{\n"
        json_text *= "\t\t\t\"title\": \"$(feature.title_meta)\",\n"
        json_text *= "\t\t\t\"library\": \"$(feature.library_meta)\",\n"
        json_text *= "\t\t\t\"name\": \"$(feature.name_meta)\",\n"
        json_text *= "\t\t\t\"name_data\": \"$(feature.name_data)\",\n"
        json_text *= "\t\t\t\"heading\": \"$(feature.heading_data)\",\n"
        json_text *= "\t\t\t\"code\": \"$(feature.code_transformed)\"\n"

        if feature == features[end]
            json_text *= "\t\t}\n"
        else
            json_text *= "\t\t},\n"
        end
    end

    json_text *= "\t]\n}"
    return json_text
end

function main()
    NUMBER_FEATURES = 433

    features = Vector{Feature}()

    ## Extraction of information
    for feature_number in 1:NUMBER_FEATURES
        # Metadata
        lines = readlines("/Users/kmilo/Dev/R/features/features_exploration/markdowns/$feature_number.md")
        title_metadata = strip(split(lines[2], ":")[2])
        library_metadata = strip(split(lines[3], ":")[2])
        name_metadata = strip(split(lines[4], ":")[2])
        
        name_data = strip(lines[7])
        heading = "<h5 id-\\\"api-references\\\">API References</h5>"
        code_data = lines[12:end-1]

        feature = Feature(title_metadata, library_metadata, name_metadata, name_data, heading, code_data, "")
        push!(features, feature)
    end

    ## Transformation of the information
    for feature in features
        ## Names
        names_divided = split(feature.name_data, " ")

        new_name = map(names_divided) do name
            if length(name) > 0
                name_divided = split(name, "->")

                noun = name_divided[1]
                verbs = name_divided[2]

                noun = replace(noun, "**"=>"")
                noun = to_html(noun, "strong")

                return noun * " -> $verbs "
            else
                return ""
            end
        end

        new_name = filter(name -> length(name) > 0, new_name)
        new_name = join(new_name, "")
        feature.name_data = "<p>" * new_name * "</p>"

        ## Code
        code_feature = feature.code_transformed

        for code_line in feature.code_data
            line_divided = split(code_line, " ")

            ## Style in the code line
            line_stylised = ""
            line_stylised *= to_html(line_divided[1], "span", "style", "color: rgb(36, 41, 47);")
            line_stylised *= to_html(" ", "span", "style", "color: rgb(36, 41, 47);")
            line_stylised *= to_html(line_divided[2], "span", "style", "color: rgb(5, 80, 174);")
            line_stylised *= to_html(" ", "span", "style", "color: rgb(36, 41, 47);")
            line_stylised *= to_html(line_divided[3], "span", "style", "color: rgb(5, 80, 174);")
            line_stylised *= to_html(" ", "span", "style", "color: rgb(36, 41, 47);")
            line_stylised *= to_html(line_divided[4], "span", "style", "color: rgb(5, 80, 174);")

            line_stylised = to_html(line_stylised, "span", "class", "line")
            code_feature *= line_stylised
        end

        code_feature = to_html(code_feature, "code")
        code_feature = to_html(code_feature, "pre")

        feature.code_transformed = code_feature
    end

    ## Generation of a single JSON
    json_text = features_to_json(features)

    ## Writing the JSON
    open("content/1.features/features.json", "w") do file
        write(file, json_text)
    end
end

main()

