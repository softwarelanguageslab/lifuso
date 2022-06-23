using StatsBase

mutable struct Feature
    title_meta::String
    library_meta::String
    name_meta::Vector{String}
    name_data::String
    heading_data::String
    code_data::Vector{String}
    code_transformed::String
end

Base.copy(x::T) where T = T([getfield(x, k) for k ∈ fieldnames(T)]...)

function search_features_libraries(allFeatures::Vector{Feature}, library::String)
    return filter(feature -> feature.library_meta == library, allFeatures)
end

function to_html(text::AbstractString, tag::String, property::String="", property_values::String="")
    if property == ""
        return "<$tag>$text</$tag>"
    else
        return "<$tag $property=\\\"$property_values\\\">$text</$tag>"
    end
end

function intersect_features(featuresA::Vector{Feature}, featuresB::Vector{Feature})
    all_namesA = Set(collect(Iterators.flatten(map(feature -> feature.name_meta, featuresA))))
    all_namesB = Set(collect(Iterators.flatten(map(feature -> feature.name_meta, featuresB))))

    names_intersection = intersect(all_namesA, all_namesB)

    function in_intersection(features::Vector{Feature})
        filtered_features = filter(features) do feature 
            length(intersect(Set(feature.name_meta), names_intersection)) > 0
        end

        filtered_features = map(filtered_features) do feature
            names_intersected = intersect(Set(feature.name_meta), names_intersection)
            feature.name_meta = collect(names_intersected)

            return feature
        end

        return filtered_features
    end

    featuresA_filtered = in_intersection(featuresA)
    featuresB_filtered = in_intersection(featuresB)
    intersected_features = [featuresA_filtered; featuresB_filtered]

    ## Filter the names on the data to be in correspondence with the metadata names
    for feature in intersected_features
        metadata_names = feature.name_meta
        filtered_metadata_names = filter(name -> length(name) > 0, metadata_names)
        nouns_names = collect(Set(map(name -> split(name, " ")[2], filtered_metadata_names)))

        new_data_names = Vector{String}()
        for noun in nouns_names
            all_verbs_noun = Vector{String}()

            for metadata_name in filtered_metadata_names
                name_divided = split(metadata_name, " ")

                if name_divided[2] == noun
                    push!(all_verbs_noun, name_divided[1])
                end
            end

            if length(all_verbs_noun) == 1
                new_data_name = "**$noun**->$(all_verbs_noun[1])"
            else
                new_data_name = "**$noun**->($(join(all_verbs_noun, ",")))"
            end

            push!(new_data_names, new_data_name)
        end

        feature.name_data = join(new_data_names, " ")
    end

    return intersected_features
end

function diff_features(featuresA::Vector{Feature}, featuresB::Vector{Feature})
    all_namesA = Set(collect(Iterators.flatten(map(feature -> feature.name_meta, featuresA))))
    all_namesB = Set(collect(Iterators.flatten(map(feature -> feature.name_meta, featuresB))))

    names_intersection = intersect(all_namesA, all_namesB)

    # This might be a source of confusion since one of the multiple names 
    # of a feature could be also in the other library being compared. 
    # What should be remarked here is that at least one of the feature names is 
    # NOT among the names of the other library.
    featuresA_filtered = filter(featuresA) do feature 
        length(intersect(Set(feature.name_meta), names_intersection)) == 0
    end

    return featuresA_filtered
end

function transform_features(features::Vector{Feature})
    transformed_features = Vector{Feature}()
    features_copy = copy(features)

    for feature in features_copy
        feature_copy = copy(feature)
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
        feature_copy.name_data = "<p>" * new_name * "</p>"

        ## Code
        code_feature = feature_copy.code_transformed

        for code_line in feature_copy.code_data
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

        feature_copy.code_transformed = code_feature

        push!(transformed_features, feature_copy)
    end

    return transformed_features
end

function features_to_json(features::Vector{Feature})
    json_text = "{\n\t\"body\": [\n"

    for feature in features
        flag_enter = false

        if length(feature.name_meta) == 1
            if length(feature.name_meta[1]) > 0
                flag_enter = true
            end
        elseif length(feature.name_meta) > 1
            flag_enter = true
        end

        if flag_enter
            json_text *= "\t\t{\n"
            json_text *= "\t\t\t\"title\": \"$(feature.title_meta)\",\n"
            json_text *= "\t\t\t\"library\": \"$(feature.library_meta)\",\n"
            json_text *= "\t\t\t\"name\": \"$(join(feature.name_meta, ","))\",\n"
            json_text *= "\t\t\t\"name_data\": \"$(feature.name_data)\",\n"
            json_text *= "\t\t\t\"heading\": \"$(feature.heading_data)\",\n"
            json_text *= "\t\t\t\"code\": \"$(feature.code_transformed)\"\n"

            if feature == features[end]
                json_text *= "\t\t}\n"
            else
                json_text *= "\t\t},\n"
            end
        end
    end

    json_text *= "\t]\n}"
    return json_text
end

function write_comparison(json_info::String, path::String, name::String)
    open("$path/$name.json", "w") do file
        write(file, json_info)
    end
end

function main()
    NUMBER_FEATURES = 433

    name_libraries = Set{String}()
    features = Vector{Feature}()

    ## Extraction of information
    for feature_number in 1:NUMBER_FEATURES
        lines = readlines("/Users/kmilo/Dev/R/features/features_exploration/markdowns/$feature_number.md")

        title_metadata = strip(split(lines[2], ":")[2])
        library_metadata = strip(split(lines[3], ":")[2])
        name_metadata = split(strip(split(lines[4], ":")[2]), ",")
        
        name_data = strip(lines[7])
        heading = "<h5 id-\\\"api-references\\\">API References</h5>"
        code_data = lines[12:end-1]

        feature = Feature(title_metadata, library_metadata, name_metadata, name_data, heading, code_data, "")
        push!(features, feature)
        push!(name_libraries, library_metadata)
    end

    for name_A in name_libraries, name_B in name_libraries
        if name_A != name_B
            println(name_A, " ", name_B)

            features_libA = search_features_libraries(features, name_A)
            features_libB = search_features_libraries(features, name_B)

            intersection_libs = intersect_features(features_libA, features_libB)
            unique_libA = diff_features(features_libA, features_libB)
            unique_libB = diff_features(features_libB, features_libA)

            transf_intersec = transform_features(intersection_libs)
            transf_uniqA = transform_features(unique_libA)
            transf_uniqB = transform_features(unique_libB)

            path_shared = "content/2.comparison/$(name_A)_$name_B/shared"
            path_unique = "content/2.comparison/$(name_A)_$name_B/unique"
            path_unique_A = "content/2.comparison/$(name_A)_$name_B/unique/$name_A"
            path_unique_B = "content/2.comparison/$(name_A)_$name_B/unique/$name_B"

            mkpath(path_shared)
            mkpath(path_unique)
            mkpath(path_unique_A)
            mkpath(path_unique_B)

            intersection_json = features_to_json(transf_intersec)
            uniqueA_json = features_to_json(transf_uniqA)
            uniqueB_json = features_to_json(transf_uniqB)

            write_comparison(intersection_json, path_shared, "shared")
            write_comparison(uniqueA_json, path_unique_A, name_A)
            write_comparison(uniqueB_json, path_unique_B, name_B)
        end
    end
end

main()
